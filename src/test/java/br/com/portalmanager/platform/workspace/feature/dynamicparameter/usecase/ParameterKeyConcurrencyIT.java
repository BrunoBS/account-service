package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyCommandService;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyQueryService;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.node.IntNode;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
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
class ParameterKeyConcurrencyIT {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ParameterKeyCommandService commands;
    @Autowired private ParameterKeyQueryService queries;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private ParameterKeyRepository repository;
    private String parent;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        reset(repository);
        parent = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO"
                    + " service_context_microservices(version,identifier,code,name,settings,lifecycle_code,created_at,updated_at)"
                    + " VALUES (0,?,'account-service','Account','{}','ACTIVE',NOW(),NOW())",
                parent);
    }

    @Test
    void uniqueConstraintProtectsConcurrentCreationWhenBothPrechecksPass() throws Exception {
        Long id =
                jdbc.queryForObject(
                        "SELECT id FROM service_context_microservices WHERE identifier=?",
                        Long.class,
                        parent);
        doReturn(false).when(repository).existsByMicroserviceIdAndKey(id, "retry");
        List<Object> results = concurrently(this::create, this::create);
        assertThat(results.stream().filter(ParameterKeyMetadata.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(DataIntegrityViolationException.class::isInstance))
                .hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parameter_keys", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void concurrentUpdatesWithTheSameVersionHaveExactlyOneWinner() throws Exception {
        ParameterKeyMetadata resource = create();
        List<Object> results =
                concurrently(
                        () ->
                                commands.update(
                                        resource.identifier(),
                                        new UpdateParameterKeyInput(
                                                0L, null, "CONFIGURATION", IntNode.valueOf(2))),
                        () ->
                                commands.update(
                                        resource.identifier(),
                                        new UpdateParameterKeyInput(
                                                0L, null, "CONFIGURATION", IntNode.valueOf(3))));
        assertThat(results.stream().filter(ParameterKeyMetadata.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(ResourceVersionConflictException.class::isInstance))
                .hasSize(1);
        assertThat(queries.findByIdentifier(resource.identifier()).version()).isEqualTo(1L);
    }

    @Test
    void updateRejectsTheStaleVersionEvenAfterAnEarlierCachedRepeatableRead() throws Exception {
        ParameterKeyMetadata resource = create();
        Long id = jdbc.queryForObject("SELECT id FROM parameter_keys", Long.class);
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> stale =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        repository.findById(id).orElseThrow();
                                                        cached.countDown();
                                                        await(changed);
                                                        commands.update(
                                                                resource.identifier(),
                                                                new UpdateParameterKeyInput(
                                                                        0L,
                                                                        null,
                                                                        "CONFIGURATION",
                                                                        IntNode.valueOf(2)));
                                                    }));
            try {
                await(cached);
                commands.update(
                        resource.identifier(),
                        new UpdateParameterKeyInput(0L, null, "CONFIGURATION", IntNode.valueOf(3)));
            } finally {
                changed.countDown();
            }
            assertThatThrownBy(() -> stale.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(queries.findByIdentifier(resource.identifier()).value().asInt()).isEqualTo(3);
    }

    @Test
    void updateWaitsForInactivationAndCannotRestoreItsClearedPublication() throws Exception {
        ParameterKeyMetadata resource = create();
        jdbc.update(
                "UPDATE parameter_keys SET"
                    + " published_value=value,published_type_code=type_code,status_code='PUBLISHED'");
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<ParameterKeyMetadata>> waiting = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                commands.inactivate(resource.identifier());
                                waiting.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return commands.update(
                                                            resource.identifier(),
                                                            new UpdateParameterKeyInput(
                                                                    1L,
                                                                    null,
                                                                    "CONFIGURATION",
                                                                    IntNode.valueOf(2)));
                                                }));
                                await(attempted);
                                assertThatThrownBy(
                                                () -> waiting.get().get(250, TimeUnit.MILLISECONDS))
                                        .isInstanceOf(TimeoutException.class);
                            });
            ParameterKeyMetadata updated = waiting.get().get(10, TimeUnit.SECONDS);
            assertThat(updated.active()).isFalse();
            assertThat(updated.version()).isEqualTo(2L);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT published_value IS NULL AND published_type_code IS NULL AND"
                                        + " status_code='DRAFT' FROM parameter_keys",
                                Boolean.class))
                .isTrue();
    }

    private ParameterKeyMetadata create() {
        return commands.create(
                new CreateParameterKeyInput(
                        null, parent, "retry", null, "CONFIGURATION", IntNode.valueOf(1)));
    }

    private List<Object> concurrently(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Object> one = executor.submit(() -> result(start, first));
            Future<Object> two = executor.submit(() -> result(start, second));
            start.countDown();
            return List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
        }
    }

    private Object result(CountDownLatch start, Callable<?> call) throws Exception {
        await(start);
        try {
            return call.call();
        } catch (ConflictException | DataIntegrityViolationException exception) {
            return exception;
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS))
                throw new AssertionError("Concurrent operation did not start");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
