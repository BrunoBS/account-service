package br.com.portalmanager.platform.workspace.feature.platform.usecase.context;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.featurecontext.FeatureContextCommandService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.CreateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.ContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.UpdateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class ContextConcurrencyIT {
    private static final String MICROSERVICE = "11111111-1111-1111-1111-111111111111";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ContextCommandService contexts;
    @Autowired private FeatureContextCommandService bindings;
    @Autowired private ContextFinder finder;
    @Autowired private FeatureCommandService features;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update("INSERT INTO platform_microservices (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES (?,'parent-service','Parent service','{}','ACTIVE',NOW(6),NOW(6))", MICROSERVICE);
    }

    @Test
    void updateReloadsACachedContextAndRejectsTheOldClientVersion() throws Exception {
        ContextOutput context = contexts.create(new CreateContextInput("review-context", "Review context", null));
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                finder.findActive(context.identifier());
                cached.countDown();
                await(changed);
                contexts.update(context.identifier(), update(context, "Stale"));
            }));
            try {
                await(cached);
                contexts.update(context.identifier(), update(context, "Current"));
            } finally { changed.countDown(); }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(jdbc.queryForObject("SELECT name FROM platform_feature_contexts", String.class)).isEqualTo("Current");
    }

    @RepeatedTest(3)
    void independentContextsCanBeCreatedWithDistinctFeaturesConcurrently() throws Exception {
        FeatureOutput first = feature("first-feature");
        FeatureOutput second = feature("second-feature");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> one = executor.submit(() -> {
                await(start);
                return createContextWithFeatures("first-context", "First context", first.identifier());
            });
            Future<?> two = executor.submit(() -> {
                await(start);
                return createContextWithFeatures("second-context", "Second context", second.identifier());
            });
            start.countDown();
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_contexts", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isEqualTo(2);
    }

    @RepeatedTest(3)
    void independentEmptyContextsCanReceiveDistinctFeaturesConcurrently() throws Exception {
        FeatureOutput first = feature("first-feature");
        FeatureOutput second = feature("second-feature");
        ContextOutput firstContext = contexts.create(new CreateContextInput("first-context", "First context", null));
        ContextOutput secondContext = contexts.create(new CreateContextInput("second-context", "Second context", null));
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> one = executor.submit(() -> { await(start); return bindings.associate(firstContext.identifier(), first.identifier()); });
            Future<?> two = executor.submit(() -> { await(start); return bindings.associate(secondContext.identifier(), second.identifier()); });
            start.countDown();
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_contexts WHERE version=1", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isEqualTo(2);
    }

    private FeatureOutput feature(String code) {
        return features.create(new CreateFeatureInput(null, code, code, null, MICROSERVICE, "{}", false));
    }
    private UpdateContextInput update(ContextOutput context, String name) {
        return new UpdateContextInput(context.version(), context.code(), name, null);
    }
    private ContextOutput createContextWithFeatures(String code, String name, String featureIdentifier) {
        ContextOutput context = contexts.create(new CreateContextInput(code, name, null));
        bindings.associate(context.identifier(), featureIdentifier);
        return ContextOutput.from(finder.findActive(context.identifier()));
    }

    @Test
    void associationWaitsForContextPurgeAndRejectsTheRemovedContextAfterCommit() throws Exception {
        ContextOutput context = contexts.create(new CreateContextInput("review-context", "Review context", null));
        FeatureOutput feature = feature("review-feature");
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<?>> pending = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                contexts.purge(context.identifier());
                pending.set(executor.submit(() -> {
                    attempted.countDown();
                    return bindings.associate(context.identifier(), feature.identifier());
                }));
                await(attempted);
                assertThatThrownBy(() -> pending.get().get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            });
            assertThatThrownBy(() -> pending.get().get(10, TimeUnit.SECONDS)).hasCauseInstanceOf(NotFoundException.class);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_contexts", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_features", Integer.class)).isEqualTo(1);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for concurrent transaction");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
