package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.config.WorkspaceProperties;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort.Reference;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyRuntimeProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ParameterKeyPublisherService {
    private static final Logger LOG = LoggerFactory.getLogger(ParameterKeyPublisherService.class);

    private final ParameterKeyRepository repository;
    private final MicroserviceReferencePort microservices;
    private final ObjectProvider<ParameterKeyCommandService> commands;
    private final ParameterKeyValidator validator;
    private final ParameterPublicationStore publicationStore;
    private final TaskExecutor executor;
    private final WorkspaceProperties properties;

    public ParameterKeyPublisherService(
            ParameterKeyRepository repository,
            MicroserviceReferencePort microservices,
            ObjectProvider<ParameterKeyCommandService> commands,
            ParameterKeyValidator validator,
            ParameterPublicationStore publicationStore,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor,
            WorkspaceProperties properties) {
        this.repository = repository;
        this.microservices = microservices;
        this.commands = commands;
        this.validator = validator;
        this.publicationStore = publicationStore;
        this.executor = executor;
        this.properties = properties;
    }

    public void publish(PublishParameterKeysInput input) {
        validator.validatePublicationIdentifiers(input == null ? null : input.identifiers());
        List<String> identifiers = List.copyOf(input.identifiers());
        UserSession session = UserContext.get().orElse(null);
        Map<String, String> context = MDC.getCopyOfContextMap();
        executor.execute(
                () -> {
                    UserSession previousSession = UserContext.get().orElse(null);
                    Map<String, String> previousContext = MDC.getCopyOfContextMap();
                    try {
                        UserContext.set(session);
                        if (context == null) MDC.clear();
                        else MDC.setContextMap(context);
                        int batchSize = properties.parameter().runtime().batchSize();
                        for (int start = 0; start < identifiers.size(); start += batchSize) {
                            List<String> published = new ArrayList<>();
                            for (String identifier :
                                    identifiers.subList(
                                            start,
                                            Math.min(start + batchSize, identifiers.size()))) {
                                try {
                                    published.add(
                                            commands.getObject()
                                                    .publishIdentifier(identifier)
                                                    .identifier());
                                } catch (RuntimeException exception) {
                                    logPublicationFailure(identifier, exception);
                                }
                            }
                            send(repository.findPublishedSnapshots(published));
                        }
                    } finally {
                        UserContext.set(previousSession);
                        if (previousContext == null) MDC.clear();
                        else MDC.setContextMap(previousContext);
                    }
                });
    }

    @Scheduled(
            fixedDelayString = "${workspace.parameter.reconciliation.interval:PT1H}",
            initialDelayString = "${workspace.parameter.reconciliation.interval:PT1H}")
    public void reconcile() {
        if (!properties.parameter().runtime().enabled()) return;
        try {
            long afterId = 0;
            while (true) {
                List<ParameterKeyRuntimeProjection> page =
                        repository
                                .findPublishedSnapshots(
                                        afterId,
                                        PageRequest.of(0, properties.parameter().runtime().batchSize()))
                                .getContent();
                if (page.isEmpty()) return;
                send(page);
                afterId = page.getLast().getId();
            }
        } catch (RuntimeException exception) {
            LOG.error("Falha na reconciliação de parâmetros dinâmicos: errorType={}",
                    exception.getClass().getName());
        }
    }

    public void remove(ParameterKeyMetadata parameterKey) {
        if (!properties.parameter().runtime().enabled()) return;
        try {
            publicationStore.remove(parameterKey.fullKey());
        } catch (RuntimeException exception) {
            LOG.error(
                    "Falha ao remover parâmetro do Redis: identifier={}, errorType={}",
                    parameterKey.identifier(),
                    exception.getClass().getName());
        }
    }

    private void send(List<ParameterKeyRuntimeProjection> snapshots) {
        if (!properties.parameter().runtime().enabled() || snapshots.isEmpty()) return;
        try {
            Map<Long, Reference> references =
                    microservices.findByInternalIds(
                            snapshots.stream()
                                    .map(ParameterKeyRuntimeProjection::getMicroserviceId)
                                    .distinct()
                                    .toList());
            Map<String, PublishedParameterValue> values = new LinkedHashMap<>();
            for (ParameterKeyRuntimeProjection snapshot : snapshots) {
                values.put(
                        references.get(snapshot.getMicroserviceId()).code()
                                + "."
                                + snapshot.getKey(),
                        new PublishedParameterValue(snapshot.getType(), snapshot.getValue()));
            }
            publicationStore.publish(values);
        } catch (RuntimeException exception) {
            for (ParameterKeyRuntimeProjection snapshot : snapshots) {
                logPublicationFailure(snapshot.getIdentifier(), exception);
            }
        }
    }

    private void logPublicationFailure(String identifier, RuntimeException exception) {
        // Exception messages, causes and parameter values may contain secrets.
        LOG.error(
                "Falha ao publicar parâmetro dinâmico: identifier={}, errorType={}",
                identifier,
                exception.getClass().getName());
    }
}
