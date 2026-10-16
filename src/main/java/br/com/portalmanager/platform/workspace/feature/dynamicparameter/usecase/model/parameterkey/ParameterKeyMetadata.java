package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey;

import br.com.portalmanager.platform.workspace.feature.dynamicparameter.domain.parameterkey.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter.Reference;

import java.time.LocalDateTime;

/** Command output and audit snapshot intentionally contain no parameter values. */
public record ParameterKeyMetadata(
        Long version,
        String identifier,
        String microserviceIdentifier,
        String key,
        String fullKey,
        String description,
        String type,
        String status,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static ParameterKeyMetadata from(ParameterKey resource, Reference reference) {
        return new ParameterKeyMetadata(
                resource.getVersion(),
                resource.getIdentifier(),
                reference.identifier(),
                resource.getKey(),
                reference.code() + "." + resource.getKey(),
                resource.getDescription(),
                resource.getType().value(),
                resource.getStatus().value(),
                resource.isActive(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
