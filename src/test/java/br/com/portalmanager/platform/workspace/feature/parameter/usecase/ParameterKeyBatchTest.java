package br.com.portalmanager.platform.workspace.feature.parameter.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort.Reference;
import br.com.portalmanager.platform.workspace.config.WorkspaceProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.config.ParameterProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyRuntimeProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyCommandService;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublisherService;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class ParameterKeyBatchTest {
    private final WorkspaceProperties properties =
            new WorkspaceProperties(
                    new ParameterProperties(
                            new ParameterProperties.Runtime(true, 500),
                            new ParameterProperties.Reconciliation(false, Duration.ofHours(1))));

    @Test
    void asynchronousPublicationPartitionsTwelveHundredIdentifiersIntoThreeRedisBatches() {
        ParameterKeyRepository repository = mock(ParameterKeyRepository.class);
        List<Runnable> tasks = new ArrayList<Runnable>();
        ParameterKeyCommandService commands = mock(ParameterKeyCommandService.class);
        ObjectProvider<ParameterKeyCommandService> commandProvider = mock(ObjectProvider.class);
        when(commandProvider.getObject()).thenReturn(commands);
        ParameterKeyPublisherService publisher =
                spy(
                        new ParameterKeyPublisherService(
                                repository,
                                mock(MicroserviceReferencePort.class),
                                commandProvider,
                                mock(ParameterKeyValidator.class),
                                mock(ParameterPublicationStore.class),
                                tasks::add,
                                properties));
        List<String> identifiers = IntStream.range(0, 1200).mapToObj(i -> "identifier-" + i).toList();
        doAnswer(
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
                                        LocalDateTime.now()))
                .when(commands)
                .publishIdentifier(anyString());
        when(repository.findPublishedSnapshots(anyCollection())).thenReturn(List.of());

        publisher.publish(new PublishParameterKeysInput(identifiers));
        verify(commands, never()).publishIdentifier(anyString());
        assertThat(tasks).hasSize(1);
        tasks.getFirst().run();

        verify(commands, times(1200)).publishIdentifier(anyString());
        ArgumentCaptor<java.util.Collection<String>> batches =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(repository, times(3)).findPublishedSnapshots(batches.capture());
        assertThat(batches.getAllValues().stream().map(java.util.Collection::size))
                .containsExactly(500, 500, 200);
        assertThat(batches.getAllValues().stream().flatMap(batch -> batch.stream()).toList())
                .containsExactlyElementsOf(identifiers);
    }

    @Test
    void schedulerUsesOneMsetPerPageAndContinuesAfterAFailedFiveHundredKeyBatch() {
        ParameterKeyRepository repository = mock(ParameterKeyRepository.class);
        MicroserviceReferencePort microservices = mock(MicroserviceReferencePort.class);
        ParameterPublicationStore redis = mock(ParameterPublicationStore.class);
        ObjectProvider<ParameterKeyCommandService> commandProvider = mock(ObjectProvider.class);
        ParameterKeyPublisherService publisher =
                new ParameterKeyPublisherService(
                        repository,
                        microservices,
                        commandProvider,
                        mock(ParameterKeyValidator.class),
                        redis,
                        Runnable::run,
                        properties);
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
                                new Reference(
                                        1L, "service-id", "service")));
        doThrow(new IllegalStateException("Redis unavailable"))
                .doNothing()
                .when(redis)
                .publish(anyMap());

        publisher.reconcile();

        ArgumentCaptor<Map<String, PublishedParameterValue>> batches =
                ArgumentCaptor.forClass(Map.class);
        verify(redis, times(3)).publish(batches.capture());
        assertThat(batches.getAllValues().stream().map(Map::size)).containsExactly(500, 500, 200);
        assertThat(batches.getAllValues().getFirst().get("service.key-1").value()).isEqualTo(value);
        assertThat(batches.getAllValues().getLast()).containsKey("service.key-1200");
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
