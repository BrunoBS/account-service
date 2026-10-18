package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicparameter.parameterkey.response;

import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;

import java.time.LocalDateTime;

public record ParameterKeyMetadataResponse(
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
    public static ParameterKeyMetadataResponse from(ParameterKeyMetadata output) {
        return new ParameterKeyMetadataResponse(
                output.version(),
                output.identifier(),
                output.microserviceIdentifier(),
                output.key(),
                output.fullKey(),
                output.description(),
                output.type(),
                output.status(),
                output.active(),
                output.createdAt(),
                output.updatedAt());
    }
}
