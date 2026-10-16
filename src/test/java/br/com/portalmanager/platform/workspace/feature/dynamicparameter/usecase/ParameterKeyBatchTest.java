package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterErrorLogger;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterRuntimeProperties;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.projection.ParameterKeyRuntimeProjection;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyPublicationOperations;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyPublicationService;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime.DynamicParameterRuntimeService;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.validation.parameterkey.ParameterKeyValidator;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.*;
import java.util.stream.IntStream;

class ParameterKeyBatchTest {
    private final DynamicParameterRuntimeProperties properties =
            new DynamicParameterRuntimeProperties(
                    new DynamicParameterRuntimeProperties.Runtime(true, 500),
                    new DynamicParameterRuntimeProperties.Reconciliation(
                            false, Duration.ofHours(1)));

    @Test
    void
            asynchronousPublicationPartitionsTwelveHundredIdentifiersIntoFiveHundredFiveHundredAndTwoHundred() {
        var operations = mock(ParameterKeyPublicationOperations.class);
        var runtime = mock(DynamicParameterRuntimeService.class);
        var validator = mock(ParameterKeyValidator.class);
        var tasks = new ArrayList<Runnable>();
        var service =
                new ParameterKeyPublicationService(
                        operations, validator, tasks::add, runtime, properties);
        var identifiers = IntStream.range(0, 1200).mapToObj(i -> "identifier-" + i).toList();
        when(operations.publish(anyString()))
                .thenAnswer(
                        invocation ->
                                new ParameterKeyMetadata(
                                        1L,
                                        invocation.getArgument(0),
                                        "service",
                                        "key",
                                        "service.key",
                                        null,
                                        "CONFIGURATION",
                                        "PUBLISHED",
                                        true,
                                        LocalDateTime.now(),
                                        LocalDateTime.now()));
        service.publish(new PublishParameterKeysInput(identifiers));
        verifyNoInteractions(operations, runtime);
        assertThat(tasks).hasSize(1);
        tasks.getFirst().run();
        ArgumentCaptor<List<String>> batches = ArgumentCaptor.forClass(List.class);
        verify(runtime, times(3)).publish(batches.capture());
        assertThat(batches.getAllValues().stream().map(List::size)).containsExactly(500, 500, 200);
        assertThat(batches.getAllValues().stream().flatMap(List::stream).toList())
                .containsExactlyElementsOf(identifiers);
        verify(operations, times(1200)).publish(anyString());
    }

    @Test
    void schedulerUsesOneMsetPerPageAndContinuesAfterAFailedFiveHundredKeyBatch() {
        var repository = mock(ParameterKeyRepository.class);
        var microservices = mock(DynamicParameterMicroserviceAdapter.class);
        var redis = mock(RedisStore.class);
        var errors = mock(DynamicParameterErrorLogger.class);
        var runtime =
                new DynamicParameterRuntimeService(
                        repository, microservices, redis, properties, errors);
        JsonNode value = JsonMapper.builder().build().readTree("{\"published\":true}");
        List<ParameterKeyRuntimeProjection> snapshots =
                IntStream.rangeClosed(1, 1200)
                        .mapToObj(
                                i ->
                                        (ParameterKeyRuntimeProjection)
                                                new Snapshot(
                                                        (long) i,
                                                        "identifier-" + i,
                                                        "key-" + i,
                                                        value))
                        .toList();
        when(repository.findPublishedSnapshots(anyLong(), any(Pageable.class)))
                .thenAnswer(
                        invocation -> {
                            int offset = ((Long) invocation.getArgument(0)).intValue();
                            int size = ((Pageable) invocation.getArgument(1)).getPageSize();
                            return new SliceImpl<>(
                                    snapshots.subList(
                                            offset, Math.min(offset + size, snapshots.size())));
                        });
        when(microservices.findByInternalIds(anyCollection()))
                .thenReturn(
                        Map.of(
                                1L,
                                new DynamicParameterMicroserviceAdapter.Reference(
                                        1L, "service-id", "service")));
        doThrow(new IllegalStateException("Redis unavailable"))
                .doNothing()
                .when(redis)
                .setAll(anyMap());
        runtime.reconcile();
        ArgumentCaptor<Map<String, DynamicParameterRuntimeService.PublishedValue>> batches =
                ArgumentCaptor.forClass(Map.class);
        verify(redis, times(3)).setAll(batches.capture());
        assertThat(batches.getAllValues().stream().map(Map::size)).containsExactly(500, 500, 200);
        assertThat(batches.getAllValues().getFirst().get("service.key-1").value()).isEqualTo(value);
        assertThat(batches.getAllValues().getLast()).containsKey("service.key-1200");
        verify(errors, times(500)).log(anyString(), anyString(), eq(value), any());
        verify(redis, never()).set(anyString(), any());
        verify(repository, never()).findRuntimeSnapshot(anyString());
    }

    private record Snapshot(Long id, String identifier, String key, JsonNode value)
            implements ParameterKeyRuntimeProjection {
        public Long getId() {
            return id;
        }

        public String getIdentifier() {
            return identifier;
        }

        public Long getMicroserviceId() {
            return 1L;
        }

        public String getKey() {
            return key;
        }

        public boolean isActive() {
            return true;
        }

        public String getType() {
            return "CONFIGURATION";
        }

        public JsonNode getValue() {
            return value;
        }
    }
}
