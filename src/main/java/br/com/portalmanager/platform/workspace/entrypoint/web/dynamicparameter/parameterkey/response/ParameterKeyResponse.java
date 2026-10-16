package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicparameter.parameterkey.response;

import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyOutput;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record ParameterKeyResponse(
        Long version,
        String identifier,
        String microserviceIdentifier,
        String key,
        String fullKey,
        String description,
        String type,
        JsonNode value,
        String status,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static ParameterKeyResponse from(ParameterKeyOutput output) {
        return new ParameterKeyResponse(
                output.version(),
                output.identifier(),
                output.microserviceIdentifier(),
                output.key(),
                output.fullKey(),
                output.description(),
                output.type(),
                output.value(),
                output.status(),
                output.active(),
                output.createdAt(),
                output.updatedAt());
    }
}
