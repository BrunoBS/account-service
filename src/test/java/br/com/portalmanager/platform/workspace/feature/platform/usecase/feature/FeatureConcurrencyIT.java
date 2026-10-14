package br.com.portalmanager.platform.workspace.feature.platform.usecase.feature;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.featurecontext.FeatureContextCommandService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.repository.context.ContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.CreateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.ContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.UpdateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceCommandService;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FeatureConcurrencyIT {
    private static final String MICROSERVICE = "11111111-1111-1111-1111-111111111111";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FeatureCommandService features;
    @Autowired private FeatureQueryService query;
    @Autowired private FeatureFinder finder;
    @Autowired private MicroserviceCommandService microservices;
    @Autowired private ContextCommandService contexts;
    @Autowired private FeatureContextCommandService bindings;
    @Autowired private ContextRepository contextRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings) VALUES ('QUARANTINED','Quarantined',3,true,'{}')");
        jdbc.update("INSERT INTO platform_microservices (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES (?,'parent-service','Parent service','{}','ACTIVE',NOW(6),NOW(6))", MICROSERVICE);
    }

    @Test
    void staleClientVersionIsComparedWithTheCurrentEntityAfterAnEarlierCachedRead() throws Exception {
        FeatureOutput feature = create();
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                finder.findActive(feature.identifier());
                cached.countDown();
                await(changed);
                features.update(feature.identifier(), update(feature, "Stale", true));
            }));
            try {
                await(cached);
                features.update(feature.identifier(), update(feature, "Current", true));
            } finally { changed.countDown(); }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(jdbc.queryForObject("SELECT name FROM platform_features WHERE identifier=?", String.class, feature.identifier())).isEqualTo("Current");
    }

    @Test
    void sharedReferenceKeepsTheParentActiveUntilTheContractTransactionCompletes() throws Exception {
        FeatureOutput feature = create();
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<?>> pending = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(query.findShareableInternalIdByIdentifier(feature.identifier())).isNotNull();
                pending.set(executor.submit(() -> {
                    attempted.countDown();
                    return microservices.inactivate(MICROSERVICE);
                }));
                await(attempted);
                assertThatThrownBy(() -> pending.get().get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            });
            pending.get().get(10, TimeUnit.SECONDS);
        }
        assertThatThrownBy(() -> query.findShareableInternalIdByIdentifier(feature.identifier())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void sharedReferenceRejectsAnInactiveFeatureEvenAfterAnEarlierCachedRead() throws Exception {
        FeatureOutput feature = create();
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                finder.findActive(feature.identifier());
                cached.countDown();
                await(changed);
                query.findShareableInternalIdByIdentifier(feature.identifier());
            }));
            try { await(cached); features.inactivate(feature.identifier()); }
            finally { changed.countDown(); }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(NotFoundException.class);
        }
    }

    @Test
    void referenceResolutionReportsConflictWhenTheFeatureMovedAfterTheParentLookupSnapshot() throws Exception {
        FeatureOutput feature = create();
        String other = "22222222-2222-2222-2222-222222222222";
        jdbc.update("INSERT INTO platform_microservices (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES (?,'other-parent','Other parent','{}','ACTIVE',NOW(6),NOW(6))", other);
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                finder.findActive(feature.identifier());
                cached.countDown();
                await(changed);
                query.findShareableInternalIdByIdentifier(feature.identifier());
            }));
            try {
                await(cached);
                features.update(feature.identifier(), new UpdateFeatureInput(0L, feature.code(), feature.name(), null, other, "{}", true));
            } finally { changed.countDown(); }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
    }

    @Test
    void associationWaitsForFeatureInactivationAndRejectsTheNewLinkAfterTheWait() throws Exception {
        FeatureOutput feature = create();
        ContextOutput context = contexts.create(new CreateContextInput("review-context", "Review context", null));
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<?>> pending = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                features.inactivate(feature.identifier());
                pending.set(executor.submit(() -> {
                    attempted.countDown();
                    return bindings.associate(context.identifier(), feature.identifier());
                }));
                await(attempted);
                assertThatThrownBy(() -> pending.get().get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            });
            assertThatThrownBy(() -> pending.get().get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(NotFoundException.class);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
    }

    @Test
    void associationReloadsAContextPreviouslyCachedAsActive() throws Exception {
        FeatureOutput feature = create();
        ContextOutput context = contexts.create(new CreateContextInput("review-context", "Review context", null));
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                contextRepository.findByIdentifier(context.identifier()).orElseThrow();
                cached.countDown();
                await(changed);
                bindings.associate(context.identifier(), feature.identifier());
            }));
            try { await(cached); contexts.inactivate(context.identifier()); }
            finally { changed.countDown(); }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ValidationException.class);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
    }

    @Test
    void retryDetectsAnAssociationCommittedAfterTheEarlierRepeatableReadSnapshot() throws Exception {
        FeatureOutput feature = create();
        ContextOutput context = contexts.create(new CreateContextInput("review-context", "Review context", null));
        CountDownLatch snapshot = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<FeatureOutput> pending = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
                snapshot.countDown();
                await(changed);
                return bindings.associate(context.identifier(), feature.identifier());
            }));
            try { await(snapshot); bindings.associate(context.identifier(), feature.identifier()); }
            finally { changed.countDown(); }
            assertThat(pending.get(10, TimeUnit.SECONDS).version()).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isEqualTo(1);
    }

    @Test
    void disablingSharingDetectsAContractCommittedAfterTheEarlierSnapshot() throws Exception {
        FeatureOutput feature = create();
        jdbc.update("INSERT IGNORE INTO type_workspaces (code,label,sort_order,is_active,settings) VALUES ('MANAGER','Manager',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_application_scopes (code,label,sort_order,is_active,settings) VALUES ('SHARED','Shared',1,true,'{}')");
        jdbc.update("INSERT INTO workspaces (id,identifier,workspace_type_code,name,description,requester,acronym,settings,email_group,lifecycle_code,created_at,updated_at) VALUES (101,UUID(),'MANAGER','Owner','Owner','owner','OWN','{}','owner@test','ACTIVE',NOW(6),NOW(6))");
        jdbc.update("INSERT INTO applications (id,identifier,workspace_id,name,alias,acronym,application_scope_code,settings,lifecycle_code,created_at,updated_at) VALUES (201,UUID(),101,'Owner app','owner-app','OWN','SHARED','{}','ACTIVE',NOW(6),NOW(6))");
        Long featureId = query.findShareableInternalIdByIdentifier(feature.identifier());
        CountDownLatch snapshot = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shared_contracts", Integer.class)).isZero();
                snapshot.countDown();
                await(changed);
                features.update(feature.identifier(), update(feature, "Disabled", false));
            }));
            try {
                await(snapshot);
                jdbc.update("INSERT INTO shared_contracts (identifier,owner_workspace_id,owner_application_id,feature_id,lifecycle_code,created_at,updated_at) VALUES (UUID(),101,201,?,'ACTIVE',NOW(6),NOW(6))", featureId);
            } finally { changed.countDown(); }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ValidationException.class);
        }
        assertThat(jdbc.queryForObject("SELECT shareable FROM platform_features WHERE id=?", Boolean.class, featureId)).isTrue();
    }

    @Test
    void contextPurgeDetectsAnAssociationCommittedAfterTheEarlierSnapshot() throws Exception {
        FeatureOutput feature = create();
        ContextOutput context = contexts.create(new CreateContextInput("review-context", "Review context", null));
        CountDownLatch snapshot = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
                snapshot.countDown();
                await(changed);
                contexts.purge(context.identifier());
            }));
            try {
                await(snapshot);
                bindings.associate(context.identifier(), feature.identifier());
                contexts.inactivate(context.identifier());
            } finally {
                changed.countDown();
            }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ValidationException.class);
        }
        assertThat(jdbc.queryForObject("SELECT lifecycle_code FROM platform_feature_contexts WHERE identifier=?", String.class,
            context.identifier())).isEqualTo("INACTIVE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isEqualTo(1);
    }

    @Test
    void contextUpdateRejectsAnEarlierVersionWithoutRemovingTheNewAssociation() throws Exception {
        FeatureOutput feature = create();
        ContextOutput context = contexts.create(new CreateContextInput("review-context", "Review context", null));
        CountDownLatch snapshot = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
                snapshot.countDown();
                await(changed);
                contexts.update(context.identifier(), new UpdateContextInput(context.version(), context.code(), "Stale change", null));
            }));
            try {
                await(snapshot);
                bindings.associate(context.identifier(), feature.identifier());
            } finally {
                changed.countDown();
            }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(jdbc.queryForObject("SELECT name FROM platform_feature_contexts WHERE identifier=?", String.class,
            context.identifier())).isEqualTo("Review context");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isEqualTo(1);
    }

    private FeatureOutput create() {
        return features.create(new CreateFeatureInput(null, "review-feature", "Review feature", null, MICROSERVICE, "{}", true));
    }

    private UpdateFeatureInput update(FeatureOutput feature, String name, boolean shareable) {
        return new UpdateFeatureInput(0L, feature.code(), name, null, MICROSERVICE, "{}", shareable);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for the concurrent transaction");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
