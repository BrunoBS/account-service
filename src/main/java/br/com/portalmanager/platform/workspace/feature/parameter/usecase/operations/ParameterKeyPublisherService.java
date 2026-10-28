package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusTypeCode;
import br.com.portalmanager.platform.workspace.feature.parameter.config.ParameterProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository.InactiveKey;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyRuntimeProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterCachePage;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort.Reference;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyQueryValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ParameterKeyPublisherService {
    private static final Logger LOG = LoggerFactory.getLogger(ParameterKeyPublisherService.class);
    private static final int MAX_ATTEMPTS = 3;
    private final ParameterKeyRepository repository;
    private final MicroserviceReferencePort microservices;
    private final ParameterKeyPublicationCommandService commands;
    private final ParameterKeyValidator validator;
    private final ParameterPublicationStore publicationStore;
    private final TaskExecutor executor;
    private final ParameterProperties properties;

    public ParameterKeyPublisherService(ParameterKeyRepository repository, MicroserviceReferencePort microservices,
            ParameterKeyPublicationCommandService commands, ParameterKeyValidator validator,
            ParameterPublicationStore publicationStore,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor, ParameterProperties properties) {
        this.repository = repository;
        this.microservices = microservices;
        this.commands = commands;
        this.validator = validator;
        this.publicationStore = publicationStore;
        this.executor = executor;
        this.properties = properties;
    }

    public void publish(PublishParameterKeysInput input) {
        validator.validatePublicationInput(input);
        // Resolve the mandatory scope before accepting work. OWNER authorization remains in the facade.
        List<Long> microserviceIds = List.copyOf(microservices.findByIdentifiers(input.microserviceIdentifiers()).keySet());
        List<String> identifiers = input.all() ? List.of("") : List.copyOf(input.identifiers());
        executeWithContext("publish", () -> publishSelected(microserviceIds, input.all(), identifiers));
    }

    public void clearCache(String microserviceIdentifier) {
        ParameterKeyQueryValidation.requireCacheMicroservice(microserviceIdentifier);
        Reference reference = microservices.findByIdentifier(microserviceIdentifier);
        executeWithContext("clearCache", () -> clearMicroserviceCache(reference));
    }

    private void executeWithContext(String operation, Runnable work) {
        UserSession session = UserContext.get().orElse(null);
        Map<String, String> context = MDC.getCopyOfContextMap();
        executor.execute(() -> {
            UserSession previousSession = UserContext.get().orElse(null);
            Map<String, String> previousContext = MDC.getCopyOfContextMap();
            try {
                UserContext.set(session);
                if (context == null) MDC.clear(); else MDC.setContextMap(context);
                work.run();
            } catch (RuntimeException failure) {
                LOG.error("Falha na operação assíncrona de parâmetros: operation={}, errorType={}",
                        operation, failure.getClass().getName());
            } finally {
                UserContext.set(previousSession);
                if (previousContext == null) MDC.clear(); else MDC.setContextMap(previousContext);
            }
        });
    }

    private void clearMicroserviceCache(Reference reference) {
        String cursor = "0";
        int batchSize = properties.runtime().batchSize();
        long processed = 0;
        long removed = 0;
        long failed = 0;
        try {
            do {
                AtomicReference<ParameterCachePage> result = new AtomicReference<>();
                String currentCursor = cursor;
                runRedis("scan", 0, () -> {
                    ParameterCachePage page = publicationStore.scan(reference.code(), currentCursor, batchSize);
                    result.set(page);
                    return page.keys().size();
                }, true);
                ParameterCachePage page = result.get();
                for (int start = 0; start < page.keys().size(); start += batchSize) {
                    List<String> keys = List.copyOf(page.keys().subList(start, Math.min(start + batchSize, page.keys().size())));
                    processed += keys.size();
                    OptionalLong deleted = runRedis("deleteAll", keys.size(), () -> publicationStore.removeAll(keys), false);
                    if (deleted.isPresent()) removed += deleted.getAsLong();
                    else failed += keys.size();
                    if (Thread.currentThread().isInterrupted()) return;
                }
                cursor = page.cursor();
            } while (!"0".equals(cursor));
            LOG.info("Limpeza de cache concluída: microserviceIdentifier={}, processed={}, removed={}, failed={}",
                    reference.identifier(), processed, removed, failed);
        } catch (RuntimeException failure) {
            LOG.error("Limpeza de cache interrompida: microserviceIdentifier={}, processed={}, removed={}, failed={}, errorType={}",
                    reference.identifier(), processed, removed, failed, failure.getClass().getName());
        }
    }

    private void publishSelected(List<Long> microserviceIds, boolean all, List<String> identifiers) {
        long afterId = 0;
        while (true) {
            List<Long> ids = repository.findPublicationIds(afterId, microserviceIds, all, identifiers,
                    PageRequest.of(0, properties.runtime().batchSize())).getContent();
            if (ids.isEmpty()) return;
            removeInactive(repository.findInactiveKeys(ids));
            try {
                int updated = commands.publishBatch(ids, microserviceIds).size();
                LOG.info("Publicação de parâmetros no MySQL: processed={}, updated={}", ids.size(), updated);
            } catch (RuntimeException failure) {
                LOG.error("Falha no lote de publicação MySQL: processed={}, errorType={}",
                        ids.size(), failure.getClass().getName());
                // Rollback prevents sending unpublished draft values. Existing snapshots remain valid.
            }
            send(repository.findPublishedSnapshotsByIds(ids, ParameterKeyStatusTypeCode.published().value()));
            afterId = ids.getLast();
        }
    }

    @Scheduled(fixedDelayString = "${workspace.parameter.reconciliation.interval:PT1H}",
            initialDelayString = "${workspace.parameter.reconciliation.interval:PT1H}")
    public void reconcile() {
        if (!properties.runtime().enabled()) return;
        try {
            long afterId = 0;
            while (true) {
                List<InactiveKey> page = repository.findInactiveKeys(afterId,
                        PageRequest.of(0, properties.runtime().batchSize())).getContent();
                if (page.isEmpty()) break;
                removeInactive(page);
                afterId = page.getLast().getId();
            }
            afterId = 0;
            while (true) {
                List<ParameterKeyRuntimeProjection> page = repository.findPublishedSnapshots(afterId,
                        PageRequest.of(0, properties.runtime().batchSize())).getContent();
                if (page.isEmpty()) return;
                send(page);
                afterId = page.getLast().getId();
            }
        } catch (RuntimeException failure) {
            LOG.error("Falha na reconciliação de parâmetros: errorType={}", failure.getClass().getName());
        }
    }

    public void remove(ParameterKeyMetadata parameterKey) {
        if (!properties.runtime().enabled()) return;
        runRedis("deleteAll", 1, () -> publicationStore.removeAll(List.of(parameterKey.fullKey())), false);
    }

    // Called before physical deletion: propagate failure so the database row survives for reconciliation.
    public void removeBeforeDeletion(ParameterKeyMetadata parameterKey) {
        runRedis("deleteAll", 1, () -> publicationStore.removeAll(List.of(parameterKey.fullKey())), true);
    }

    private void removeInactive(List<InactiveKey> keys) {
        if (!properties.runtime().enabled() || keys.isEmpty()) return;
        Map<Long, Reference> references = references(keys.stream().map(InactiveKey::getMicroserviceId).toList());
        List<String> names = keys.stream().map(key -> references.get(key.getMicroserviceId()).code()
                + "." + key.getKey()).toList();
        runRedis("deleteAll", names.size(), () -> publicationStore.removeAll(names), false);
    }

    private void send(List<ParameterKeyRuntimeProjection> snapshots) {
        if (!properties.runtime().enabled() || snapshots.isEmpty()) return;
        Map<Long, Reference> references = references(snapshots.stream()
                .map(ParameterKeyRuntimeProjection::getMicroserviceId).toList());
        Map<String, PublishedParameterValue> values = new LinkedHashMap<>();
        for (ParameterKeyRuntimeProjection snapshot : snapshots) {
            values.put(references.get(snapshot.getMicroserviceId()).code() + "." + snapshot.getKey(),
                    new PublishedParameterValue(snapshot.getType(), snapshot.getValue()));
        }
        runRedis("setAll", values.size(), () -> {
            publicationStore.publish(values);
            return values.size();
        }, false);
    }

    private Map<Long, Reference> references(Collection<Long> ids) {
        return microservices.findByInternalIds(ids.stream().distinct().toList());
    }

    private OptionalLong runRedis(String operation, int count, LongSupplier action, boolean propagate) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                long affected = action.getAsLong();
                LOG.info("Reconciliação Redis: operation={}, processed={}, affected={}, attempt={}",
                        operation, count, affected, attempt);
                return OptionalLong.of(affected);
            } catch (RuntimeException failure) {
                boolean retryable = failure instanceof ApiException api
                        && ("redis.unavailable".equals(api.getMessageKey()) || "redis.timeout".equals(api.getMessageKey()));
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    LOG.error("Falha no lote Redis: operation={}, processed={}, failed={}, attempts={}, errorType={}",
                            operation, count, count, attempt, failure.getClass().getName());
                    if (propagate) throw failure;
                    return OptionalLong.empty();
                }
                LOG.warn("Nova tentativa do lote Redis: operation={}, processed={}, attempt={}, errorType={}",
                        operation, count, attempt, failure.getClass().getName());
                try {
                    Thread.sleep(properties.runtime().retryDelay().multipliedBy(attempt));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    LOG.error("Lote Redis interrompido: operation={}, failed={}", operation, count);
                    if (propagate) throw failure;
                    return OptionalLong.empty();
                }
            }
        }
        return OptionalLong.empty();
    }
}
