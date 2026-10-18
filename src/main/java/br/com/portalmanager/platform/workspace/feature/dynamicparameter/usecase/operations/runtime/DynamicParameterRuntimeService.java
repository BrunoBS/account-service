package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter.Reference;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterErrorLogger;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterRuntimeProperties;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.projection.ParameterKeyRuntimeProjection;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Publishes only committed snapshots; the hourly job reads the database, never scans Redis. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DynamicParameterRuntimeService {
    private final ParameterKeyRepository repository;
    private final DynamicParameterMicroserviceAdapter microservices;
    private final RedisStore redis;
    private final DynamicParameterRuntimeProperties properties;
    private final DynamicParameterErrorLogger errors;

    public DynamicParameterRuntimeService(
            ParameterKeyRepository repository,
            DynamicParameterMicroserviceAdapter microservices,
            RedisStore redis,
            DynamicParameterRuntimeProperties properties,
            DynamicParameterErrorLogger errors) {
        this.repository = repository;
        this.microservices = microservices;
        this.redis = redis;
        this.properties = properties;
        this.errors = errors;
    }

    public void publish(List<String> identifiers) {
        if (!properties.runtime().enabled() || identifiers.isEmpty()) return;
        try {
            send(repository.findPublishedSnapshots(identifiers));
        } catch (RuntimeException exception) {
            identifiers.forEach(identifier -> logPublicationFailure(identifier, exception));
        }
    }

    public void remove(ParameterKeyMetadata parameterKey) {
        if (!properties.runtime().enabled()) return;
        try {
            redis.delete(parameterKey.fullKey());
        } catch (RuntimeException exception) {
            errors.log(parameterKey.identifier(), parameterKey.fullKey(), null, exception);
        }
    }

    private void send(List<ParameterKeyRuntimeProjection> snapshots) {
        if (snapshots.isEmpty()) return;
        Map<Long, Reference> references = Map.of();
        try {
            references =
                    microservices.findByInternalIds(
                            snapshots.stream()
                                    .map(ParameterKeyRuntimeProjection::getMicroserviceId)
                                    .distinct()
                                    .toList());
            Map<String, PublishedValue> values = new LinkedHashMap<>();
            for (var snapshot : snapshots) {
                values.put(
                        references.get(snapshot.getMicroserviceId()).code()
                                + "."
                                + snapshot.getKey(),
                        new PublishedValue(snapshot.getType(), snapshot.getValue()));
            }
            redis.setAll(values);
        } catch (RuntimeException exception) {
            // MSET fails the whole batch. Keep committed snapshots for the next hourly run.
            for (var snapshot : snapshots) {
                var reference = references.get(snapshot.getMicroserviceId());
                String fullKey =
                        reference == null ? null : reference.code() + "." + snapshot.getKey();
                errors.log(snapshot.getIdentifier(), fullKey, snapshot.getValue(), exception);
            }
        }
    }

    public void logPublicationFailure(String identifier, RuntimeException exception) {
        String fullKey = null;
        JsonNode publishedValue = null;
        try {
            var state = repository.findRuntimeSnapshot(identifier);
            if (state.isPresent()) {
                var snapshot = state.get();
                publishedValue = snapshot.getValue();
                var reference =
                        microservices
                                .findByInternalIds(java.util.List.of(snapshot.getMicroserviceId()))
                                .get(snapshot.getMicroserviceId());
                fullKey = reference.code() + "." + snapshot.getKey();
            }
        } catch (RuntimeException lookupFailure) {
            // Keep the original failure even if its context cannot be loaded.
        }
        errors.log(identifier, fullKey, publishedValue, exception);
    }

    public void reconcile() {
        if (!properties.runtime().enabled()) return;
        try {
            long afterId = 0;
            while (true) {
                var page =
                        repository
                                .findPublishedSnapshots(
                                        afterId,
                                        PageRequest.of(0, properties.runtime().batchSize()))
                                .getContent();
                if (page.isEmpty()) return;
                send(page);
                afterId = page.getLast().getId();
            }
        } catch (RuntimeException exception) {
            errors.log(null, null, null, exception);
        }
    }

    public record PublishedValue(String type, JsonNode value) {}
}
