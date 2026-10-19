package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.featuregroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.CreateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.FeatureGroupOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.UpdateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupAssociationCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupFinder;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FeatureGroupConcurrencyIT {
    private static final String MICROSERVICE = "11111111-1111-1111-1111-111111111111";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FeatureGroupCommandService groups;
    @Autowired private FeatureGroupAssociationCommandService bindings;
    @Autowired private FeatureGroupFinder finder;
    @Autowired private FeatureCommandService features;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
                "INSERT INTO service_context_microservices"
                    + " (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES"
                    + " (?,'parent-service','Parent service','{}','ACTIVE',NOW(6),NOW(6))",
                MICROSERVICE);
    }

    @Test
    void updateReloadsACachedGroupAndRejectsTheOldClientVersion() throws Exception {
        FeatureGroupOutput group =
                groups.create(new CreateFeatureGroupInput("review-group", "Review group", null));
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        finder.findActive(group.identifier());
                                                        cached.countDown();
                                                        await(changed);
                                                        groups.update(
                                                                group.identifier(),
                                                                update(group, "Stale"));
                                                    }));
            try {
                await(cached);
                groups.update(group.identifier(), update(group, "Current"));
            } finally {
                changed.countDown();
            }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(jdbc.queryForObject("SELECT name FROM feature_group", String.class))
                .isEqualTo("Current");
    }

    @RepeatedTest(3)
    void independentContextsCanBeCreatedWithDistinctFeaturesConcurrently() throws Exception {
        FeatureOutput first = feature("first-feature");
        FeatureOutput second = feature("second-feature");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> one =
                    executor.submit(
                            () -> {
                                await(start);
                                return createGroupWithFeatures(
                                        "first-group", "First group", first.identifier());
                            });
            Future<?> two =
                    executor.submit(
                            () -> {
                                await(start);
                                return createGroupWithFeatures(
                                        "second-group", "Second group", second.identifier());
                            });
            start.countDown();
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group", Integer.class))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group_feature",
                                Integer.class))
                .isEqualTo(2);
    }

    @RepeatedTest(3)
    void independentEmptyContextsCanReceiveDistinctFeaturesConcurrently() throws Exception {
        FeatureOutput first = feature("first-feature");
        FeatureOutput second = feature("second-feature");
        FeatureGroupOutput firstContext =
                groups.create(new CreateFeatureGroupInput("first-group", "First group", null));
        FeatureGroupOutput secondContext =
                groups.create(new CreateFeatureGroupInput("second-group", "Second group", null));
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> one =
                    executor.submit(
                            () -> {
                                await(start);
                                return bindings.associate(
                                        firstContext.identifier(), first.identifier());
                            });
            Future<?> two =
                    executor.submit(
                            () -> {
                                await(start);
                                return bindings.associate(
                                        secondContext.identifier(), second.identifier());
                            });
            start.countDown();
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group WHERE version=1",
                                Integer.class))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group_feature",
                                Integer.class))
                .isEqualTo(2);
    }

    private FeatureOutput feature(String code) {
        return features.create(
                new CreateFeatureInput(null, code, code, null, MICROSERVICE, "{}", false));
    }

    private UpdateFeatureGroupInput update(FeatureGroupOutput group, String name) {
        return new UpdateFeatureGroupInput(group.version(), group.code(), name, null);
    }

    private FeatureGroupOutput createGroupWithFeatures(
            String code, String name, String featureIdentifier) {
        FeatureGroupOutput group = groups.create(new CreateFeatureGroupInput(code, name, null));
        bindings.associate(group.identifier(), featureIdentifier);
        return new br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.featuregroup.FeatureGroupMapper()
                .toOutput(finder.findActive(group.identifier()));
    }

    @Test
    void associationWaitsForGroupPurgeAndRejectsTheRemovedContextAfterCommit() throws Exception {
        FeatureGroupOutput group =
                groups.create(new CreateFeatureGroupInput("review-group", "Review group", null));
        FeatureOutput feature = feature("review-feature");
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<?>> pending = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                groups.purge(group.identifier());
                                pending.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return bindings.associate(
                                                            group.identifier(),
                                                            feature.identifier());
                                                }));
                                await(attempted);
                                assertThatThrownBy(
                                                () -> pending.get().get(200, TimeUnit.MILLISECONDS))
                                        .isInstanceOf(TimeoutException.class);
                            });
            assertThatThrownBy(() -> pending.get().get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(NotFoundException.class);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group", Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group_feature",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM service_context_features", Integer.class))
                .isEqualTo(1);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS))
                throw new AssertionError("Timed out waiting for concurrent transaction");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
