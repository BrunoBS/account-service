package br.com.portalmanager.platform.workspace.feature.parameter.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.workspace.config.WorkspaceProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.config.ParameterProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyRuntimeProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyCommandService;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublisherService;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import tools.jackson.databind.json.JsonMapper;

class ParameterKeyPublisherLoggingTest {
    private static final String SECRET = "synthetic-sensitive-value-93f7";
    private final ParameterKeyRepository repository = mock(ParameterKeyRepository.class);
    private final MicroserviceReferencePort microservices = mock(MicroserviceReferencePort.class);
    private final ParameterPublicationStore store = mock(ParameterPublicationStore.class);
    private final ParameterKeyCommandService commands = mock(ParameterKeyCommandService.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(ParameterKeyPublisherService.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private ParameterKeyPublisherService publisher;

    @BeforeEach
    void setUp() {
        ObjectProvider<ParameterKeyCommandService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(commands);
        publisher = new ParameterKeyPublisherService(repository, microservices, provider,
                mock(ParameterKeyValidator.class), store, Runnable::run,
                new WorkspaceProperties(new ParameterProperties(
                        new ParameterProperties.Runtime(true, 500),
                        new ParameterProperties.Reconciliation(true, Duration.ofHours(1)))));
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void redisFailureDoesNotLogScalarPayloadOrExceptionChain() {
        ParameterKeyRuntimeProjection snapshot = mock(ParameterKeyRuntimeProjection.class);
        when(snapshot.getId()).thenReturn(1L);
        when(snapshot.getIdentifier()).thenReturn("parameter-id");
        when(snapshot.getMicroserviceId()).thenReturn(2L);
        when(snapshot.getKey()).thenReturn("key");
        when(snapshot.getType()).thenReturn("CONFIGURATION");
        when(snapshot.getValue()).thenReturn(JsonMapper.builder().build().getNodeFactory().textNode(SECRET));
        when(repository.findPublishedSnapshots(anyLong(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(snapshot)), new SliceImpl<>(List.of()));
        when(microservices.findByInternalIds(anyCollection())).thenReturn(Map.of(
                2L, new MicroserviceReferencePort.Reference(2L, "service-id", "service")));
        doThrow(sensitiveFailure()).when(store).publish(anyMap());

        publisher.reconcile();

        assertSafeFailure();
        verify(store).publish(anyMap());
    }

    @Test
    void commandFailureDoesNotLoadOrLogPublishedValue() {
        when(commands.publishIdentifier("parameter-id")).thenThrow(sensitiveFailure());
        when(repository.findPublishedSnapshots(anyCollection())).thenReturn(List.of());

        publisher.publish(new PublishParameterKeysInput(List.of("parameter-id")));

        assertSafeFailure();
        verify(repository, never()).findRuntimeSnapshot(anyString());
    }

    @Test
    void reconciliationFailureDoesNotLogRepositoryException() {
        when(repository.findPublishedSnapshots(anyLong(), any(Pageable.class)))
                .thenThrow(sensitiveFailure());

        publisher.reconcile();

        assertSafeFailure();
    }

    @Test
    void removalFailureDoesNotLogKeyOrException() {
        ParameterKeyMetadata parameter = mock(ParameterKeyMetadata.class);
        when(parameter.identifier()).thenReturn("parameter-id");
        when(parameter.fullKey()).thenReturn(SECRET);
        doThrow(sensitiveFailure()).when(store).remove(SECRET);

        publisher.remove(parameter);

        assertSafeFailure();
    }

    private RuntimeException sensitiveFailure() {
        RuntimeException failure = new IllegalStateException(SECRET, new RuntimeException(SECRET));
        failure.addSuppressed(new RuntimeException(SECRET));
        return failure;
    }

    private void assertSafeFailure() {
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).contains("errorType=java.lang.IllegalStateException")
                .doesNotContain(SECRET, "publishedValue");
        assertThat(event.getArgumentArray()).allSatisfy(argument -> {
            assertThat(argument).isNotInstanceOf(Throwable.class);
            assertThat(String.valueOf(argument)).doesNotContain(SECRET);
        });
        assertThat(event.getThrowableProxy()).isNull();
    }
}
