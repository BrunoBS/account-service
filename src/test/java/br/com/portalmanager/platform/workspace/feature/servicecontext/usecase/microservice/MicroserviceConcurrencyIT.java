package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.microservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.UpdateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceFinder;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import org.junit.jupiter.api.BeforeEach;
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
class MicroserviceConcurrencyIT {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MicroserviceCommandService microservices;
    @Autowired private MicroserviceFinder finder;
    @Autowired private FeatureCommandService features;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings)"
                        + " VALUES ('QUARANTINED','Quarantined',3,true,'{}')");
    }

    @Test
    void inactivationWaitsUntilTheNewFeatureTransactionCommits() throws Exception {
        MicroserviceOutput parent = microservices.create(input("review-service", "Review service"));
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<MicroserviceOutput>> pending = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                features.create(feature(parent.identifier()));
                                pending.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return microservices.inactivate(
                                                            parent.identifier());
                                                }));
                                await(attempted);
                                assertThatThrownBy(
                                                () -> pending.get().get(200, TimeUnit.MILLISECONDS))
                                        .isInstanceOf(TimeoutException.class);
                            });
            assertThat(pending.get().get(10, TimeUnit.SECONDS).lifecycle()).isEqualTo("INACTIVE");
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM service_context_features", Integer.class))
                .isEqualTo(1);
        assertThatThrownBy(
                        () ->
                                features.create(
                                        new CreateFeatureInput(
                                                null,
                                                "other",
                                                "Other",
                                                null,
                                                parent.identifier(),
                                                "{}",
                                                false)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void newBindingWaitsForInactivationAndRejectsTheInactiveParentAfterTheWait() throws Exception {
        MicroserviceOutput parent = microservices.create(input("review-service", "Review service"));
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<?>> pending = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                microservices.inactivate(parent.identifier());
                                pending.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return features.create(
                                                            feature(parent.identifier()));
                                                }));
                                await(attempted);
                                assertThatThrownBy(
                                                () -> pending.get().get(200, TimeUnit.MILLISECONDS))
                                        .isInstanceOf(TimeoutException.class);
                            });
            assertThatThrownBy(() -> pending.get().get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ValidationException.class);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM service_context_features", Integer.class))
                .isZero();
    }

    @Test
    void updateChecksTheCurrentVersionEvenWhenTheEntityWasAlreadyCached() throws Exception {
        MicroserviceOutput parent = microservices.create(input("review-service", "Review service"));
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        finder.findActive(parent.identifier());
                                                        cached.countDown();
                                                        await(updated);
                                                        microservices.update(
                                                                parent.identifier(),
                                                                new UpdateMicroserviceInput(
                                                                        0L,
                                                                        parent.code(),
                                                                        "Stale",
                                                                        null,
                                                                        "{}"));
                                                    }));
            try {
                await(cached);
                microservices.update(
                        parent.identifier(),
                        new UpdateMicroserviceInput(0L, parent.code(), "Current", null, "{}"));
            } finally {
                updated.countDown();
            }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT name FROM service_context_microservices WHERE identifier=?",
                                String.class,
                                parent.identifier()))
                .isEqualTo("Current");
    }

    private CreateMicroserviceInput input(String code, String name) {
        return new CreateMicroserviceInput(null, code, name, null, "{}");
    }

    private CreateFeatureInput feature(String identifier) {
        return new CreateFeatureInput(null, "review", "Review", null, identifier, "{}", false);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS))
                throw new AssertionError("Timed out waiting for the concurrent transaction");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
