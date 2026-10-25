package br.com.portalmanager.platform.workspace.feature.parameter.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyCommandService;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyQueryService;
import br.com.portalmanager.platform.workspace.feature.parameter.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultScenario;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.MicroserviceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.node.IntNode;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDateTime;
import tools.jackson.databind.json.JsonMapper;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ParameterKeyConcurrencyIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{
    @Autowired private ParameterKeyCommandService commands;
    @Autowired private ParameterKeyQueryService queries;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PlatformRequestSpecificationFactory requests;
    @Autowired private SchemaDefaultScenario schemaDefaults;
    @Autowired private MicroserviceRepository microservices;
    private String parent;

    @BeforeEach
    void prepare() {
        schemaDefaults.setup();
        reset(parameterKeyRepository);
        parent = new MicroserviceClient(requests, JsonMapper.builder().build())
                .createParent().expectCreated().identifier();
    }

    @Test
    void uniqueConstraintProtectsConcurrentCreationWhenBothPrechecksPass() throws Exception {
        Long id = microservices.findInternalIdByIdentifier(parent).orElseThrow();
        doReturn(false).when(parameterKeyRepository).existsByMicroserviceIdAndKey(id, "retry");
        List<Object> results = concurrently(this::create, this::create);
        assertThat(results.stream().filter(ParameterKeyMetadata.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(DataIntegrityViolationException.class::isInstance))
                .hasSize(1);
        assertThat(parameterKeyRepository.count()).isEqualTo(1);
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
        Long id = parameterKeyRepository.findAll().getFirst().getId();
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            Future<?> stale =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        parameterKeyRepository.findById(id).orElseThrow();
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
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<ParameterKeyMetadata>> waiting = new AtomicReference<>();
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                var parameter = parameterKeyRepository
                                        .findForUpdate(resource.identifier()).orElseThrow();
                                parameter.publish(LocalDateTime.now());
                                parameterKeyRepository.saveAndFlush(parameter);
                                commands.inactivate(resource.identifier());
                                waiting.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return commands.update(
                                                            resource.identifier(),
                                                            new UpdateParameterKeyInput(
                                                                    2L,
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
            assertThat(updated.version()).isEqualTo(3L);
        }
        var snapshot = parameterKeyRepository.findRuntimeSnapshot(resource.identifier()).orElseThrow();
        assertThat(snapshot.isActive()).isFalse();
        assertThat(snapshot.getValue()).isNull();
        assertThat(queries.findByIdentifier(resource.identifier()).status()).isEqualTo("DRAFT");
    }

    private ParameterKeyMetadata create() {
        return commands.create(
                new CreateParameterKeyInput(
                        null, parent, "retry", null, "CONFIGURATION", IntNode.valueOf(1)));
    }

    private List<Object> concurrently(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newFixedThreadPool(2))) {
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
