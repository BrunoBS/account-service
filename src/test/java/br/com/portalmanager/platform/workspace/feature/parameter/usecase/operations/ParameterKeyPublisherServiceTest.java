package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.feature.parameter.config.ParameterProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyRuntimeProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.SliceImpl;
import tools.jackson.databind.json.JsonMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ParameterKeyPublisherServiceTest {
    private final ParameterKeyRepository repository = mock(ParameterKeyRepository.class);
    private final MicroserviceReferencePort microservices = mock(MicroserviceReferencePort.class);
    private final ParameterKeyPublicationCommandService commands = mock(ParameterKeyPublicationCommandService.class);
    private final ParameterKeyValidator validator = mock(ParameterKeyValidator.class);
    private final ParameterPublicationStore store = mock(ParameterPublicationStore.class);
    private final ParameterProperties properties = new ParameterProperties(
            new ParameterProperties.Runtime(true, 2, Duration.ZERO),
            new ParameterProperties.Reconciliation(true, Duration.ofHours(1)));

    private ParameterKeyPublisherService publisher(TaskExecutor executor) {
        when(microservices.findByIdentifiers(anyCollection())).thenReturn(Map.of(7L,
                new MicroserviceReferencePort.Reference(7L, "microservice-id", "service")));
        when(microservices.findByInternalIds(anyCollection())).thenReturn(Map.of(7L,
                new MicroserviceReferencePort.Reference(7L, "microservice-id", "service")));
        return new ParameterKeyPublisherService(repository, microservices, commands, validator, store, executor, properties);
    }

    private ParameterKeyRuntimeProjection snapshot(long id) {
        ParameterKeyRuntimeProjection result = mock(ParameterKeyRuntimeProjection.class);
        when(result.getId()).thenReturn(id);
        when(result.getMicroserviceId()).thenReturn(7L);
        when(result.getKey()).thenReturn("key-" + id);
        when(result.getType()).thenReturn("CONFIGURATION");
        when(result.getValue()).thenReturn(JsonMapper.builder().build().readTree("1"));
        return result;
    }

    private void selectTwoBatches() {
        List<ParameterKeyRuntimeProjection> first = List.of(snapshot(1), snapshot(2));
        List<ParameterKeyRuntimeProjection> second = List.of(snapshot(3));
        when(repository.findPublicationIds(anyLong(), anyCollection(), anyBoolean(), anyCollection(), any()))
                .thenReturn(new SliceImpl<>(List.of(1L, 2L)), new SliceImpl<>(List.of(3L)), new SliceImpl<>(List.of()));
        when(repository.findPublishedSnapshotsByIds(List.of(1L, 2L), "PUBLISHED"))
                .thenReturn(first);
        when(repository.findPublishedSnapshotsByIds(List.of(3L), "PUBLISHED"))
                .thenReturn(second);
    }

    @Test void retriesTheSamePreparedBatchThreeTimesThenContinuesWithoutRepeatingMysqlWork() {
        ParameterKeyPublisherService publisher = publisher(Runnable::run);
        selectTwoBatches();
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> { if (calls.incrementAndGet() <= 3) throw new ApiException("redis.timeout"); return null; })
                .when(store).publish(anyMap());
        publisher.publish(new PublishParameterKeysInput(List.of(), true, List.of("microservice-id")));
        ArgumentCaptor<Map<String, PublishedParameterValue>> captured = ArgumentCaptor.forClass(Map.class);
        verify(store, times(4)).publish(captured.capture());
        assertThat(captured.getAllValues().get(0)).hasSize(2)
                .isSameAs(captured.getAllValues().get(1)).isSameAs(captured.getAllValues().get(2));
        assertThat(captured.getAllValues().get(3)).hasSize(1);
        verify(commands, times(1)).publishBatch(List.of(1L, 2L), List.of(7L));
        verify(commands, times(1)).publishBatch(List.of(3L), List.of(7L));
        verify(repository, times(2)).findPublishedSnapshotsByIds(anyCollection(), eq("PUBLISHED"));
    }

    @Test void deterministicRedisFailuresAreNotRetriedAndNextBatchStillRuns() {
        ParameterKeyPublisherService publisher = publisher(Runnable::run);
        selectTwoBatches();
        doThrow(new ApiException("redis.serialization")).doNothing().when(store).publish(anyMap());
        publisher.publish(new PublishParameterKeysInput(List.of(), true, List.of("microservice-id")));
        verify(store, times(2)).publish(anyMap());
    }

    @Test void schedulerRetriesInactiveBatchesAndContinuesToPublishedBatches() {
        ParameterKeyPublisherService publisher = publisher(Runnable::run);
        ParameterKeyRepository.InactiveKey inactive = mock(ParameterKeyRepository.InactiveKey.class);
        when(inactive.getId()).thenReturn(1L);
        when(inactive.getMicroserviceId()).thenReturn(7L);
        when(inactive.getKey()).thenReturn("inactive");
        when(repository.findInactiveKeys(anyLong(), any())).thenReturn(new SliceImpl<>(List.of(inactive)), new SliceImpl<>(List.of()));
        ParameterKeyRuntimeProjection published = snapshot(2);
        when(repository.findPublishedSnapshots(anyLong(), any())).thenReturn(new SliceImpl<>(List.of(published)), new SliceImpl<>(List.of()));
        when(store.removeAll(anyCollection())).thenThrow(new ApiException("redis.unavailable"));
        publisher.reconcile();
        verify(store, times(3)).removeAll(List.of("service.inactive"));
        verify(store).publish(anyMap());
        verifyNoInteractions(commands);
    }

    @Test void deletionPropagatesExhaustedCommunicationFailureButAbsenceSucceeds() {
        ParameterKeyPublisherService publisher = publisher(Runnable::run);
        ParameterKeyMetadata metadata = new ParameterKeyMetadata(0L, "id", "ms", "key", "service.key",
                null, "CONFIGURATION", "DRAFT", false, null, null);
        when(store.removeAll(List.of("service.key"))).thenThrow(new ApiException("redis.unavailable"));
        assertThatThrownBy(() -> publisher.removeBeforeDeletion(metadata)).hasMessage("redis.unavailable");
        verify(store, times(3)).removeAll(List.of("service.key"));
        reset(store);
        when(store.removeAll(List.of("service.key"))).thenReturn(0L);
        assertThatCode(() -> publisher.removeBeforeDeletion(metadata)).doesNotThrowAnyException();
    }

    @Test void asynchronousWorkRestoresWorkerMdcAfterUsingRequestCorrelation() {
        TaskExecutor executor = mock(TaskExecutor.class);
        ParameterKeyPublisherService publisher = publisher(executor);
        when(repository.findPublicationIds(anyLong(), anyCollection(), anyBoolean(), anyCollection(), any()))
                .thenAnswer(invocation -> {
                    assertThat(MDC.get("correlationId")).isEqualTo("request-correlation");
                    return new SliceImpl<Long>(List.of());
                });
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            MDC.put("correlationId", "request-correlation");
            publisher.publish(new PublishParameterKeysInput(List.of(), true, List.of("microservice-id")));
            ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
            verify(executor).execute(task.capture());
            MDC.put("correlationId", "worker-correlation");
            task.getValue().run();
            assertThat(MDC.get("correlationId")).isEqualTo("worker-correlation");
        } finally { if (previous == null) MDC.clear(); else MDC.setContextMap(previous); }
    }
}
