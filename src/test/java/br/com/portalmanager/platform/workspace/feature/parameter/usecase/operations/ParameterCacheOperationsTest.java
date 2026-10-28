package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.feature.parameter.config.ParameterProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterCachePage;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.core.task.TaskExecutor;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ParameterCacheOperationsTest {
    private final ParameterKeyRepository repository = mock(ParameterKeyRepository.class);
    private final MicroserviceReferencePort microservices = mock(MicroserviceReferencePort.class);
    private final ParameterKeyPublicationCommandService commands = mock(ParameterKeyPublicationCommandService.class);
    private final ParameterKeyValidator validator = mock(ParameterKeyValidator.class);
    private final ParameterPublicationStore store = mock(ParameterPublicationStore.class);
    private final ParameterProperties properties = new ParameterProperties(
            new ParameterProperties.Runtime(true, 2, Duration.ZERO),
            new ParameterProperties.Reconciliation(true, Duration.ofHours(1)));

    private ParameterKeyPublisherService publisher(TaskExecutor executor) {
        when(microservices.findByIdentifier("microservice-id")).thenReturn(
                new MicroserviceReferencePort.Reference(7L, "microservice-id", "service"));
        return new ParameterKeyPublisherService(repository, microservices, commands, validator, store, executor, properties);
    }

    @Test void emptyScanPageDoesNotStopTraversalAndOversizedScanReplyIsDeletedInBoundedBatches() {
        ParameterKeyPublisherService publisher = publisher(Runnable::run);
        when(store.scan("service", "0", 2)).thenReturn(new ParameterCachePage("7", List.of()));
        when(store.scan("service", "7", 2)).thenReturn(new ParameterCachePage("0",
                List.of("service.a", "service.b", "service.c", "service.d", "service.e")));
        when(store.removeAll(anyCollection())).thenAnswer(invocation -> (long) invocation.<Collection<String>>getArgument(0).size());
        publisher.clearCache("microservice-id");
        verify(store).removeAll(List.of("service.a", "service.b"));
        verify(store).removeAll(List.of("service.c", "service.d"));
        verify(store).removeAll(List.of("service.e"));
        verifyNoInteractions(repository, commands, validator);
    }

    @Test void failedDeleteBatchRetriesTheSameKeysAndContinuesWithoutRescanning() {
        ParameterKeyPublisherService publisher = publisher(Runnable::run);
        when(store.scan("service", "0", 2)).thenReturn(new ParameterCachePage("0",
                List.of("service.a", "service.b", "service.c")));
        AtomicInteger attempts = new AtomicInteger();
        when(store.removeAll(anyCollection())).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() <= 3) throw new ApiException("redis.unavailable");
            return 1L;
        });
        publisher.clearCache("microservice-id");
        ArgumentCaptor<Collection<String>> keys = ArgumentCaptor.forClass(Collection.class);
        verify(store, times(4)).removeAll(keys.capture());
        assertThat(keys.getAllValues().get(0)).isSameAs(keys.getAllValues().get(1)).isSameAs(keys.getAllValues().get(2));
        assertThat(keys.getAllValues().get(3)).containsExactly("service.c");
        verify(store, times(1)).scan("service", "0", 2);
        verifyNoInteractions(repository, commands);
    }

    @Test void transientScanFailureRetriesTheSameCursorAndStopsAfterThreeAttempts() {
        ParameterKeyPublisherService publisher = publisher(Runnable::run);
        when(store.scan("service", "0", 2)).thenThrow(new ApiException("redis.timeout"));
        publisher.clearCache("microservice-id");
        verify(store, times(3)).scan("service", "0", 2);
        verify(store, never()).removeAll(anyCollection());
    }

    @Test void deterministicScanFailureIsNotRetried() {
        ParameterKeyPublisherService publisher = publisher(Runnable::run);
        when(store.scan("service", "0", 2)).thenThrow(new ApiException("redis.validation"));
        publisher.clearCache("microservice-id");
        verify(store, times(1)).scan("service", "0", 2);
    }

    @Test void workIsQueuedAfterResolvingTheScopeAndRestoresWorkerCorrelation() {
        TaskExecutor executor = mock(TaskExecutor.class);
        ParameterKeyPublisherService publisher = publisher(executor);
        when(store.scan("service", "0", 2)).thenAnswer(invocation -> {
            assertThat(MDC.get("correlationId")).isEqualTo("cache-request");
            return new ParameterCachePage("0", List.of());
        });
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            MDC.put("correlationId", "cache-request");
            publisher.clearCache("microservice-id");
            verifyNoInteractions(store);
            ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
            verify(executor).execute(task.capture());
            MDC.put("correlationId", "worker");
            task.getValue().run();
            assertThat(MDC.get("correlationId")).isEqualTo("worker");
        } finally { if (previous == null) MDC.clear(); else MDC.setContextMap(previous); }
    }
}
