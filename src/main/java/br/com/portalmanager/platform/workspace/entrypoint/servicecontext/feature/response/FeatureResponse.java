package br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.response;

import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record FeatureResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String microserviceIdentifier,
        String microserviceCode,
        String lifecycle,
        boolean shareable,
        JsonNode settings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static FeatureResponse from(FeatureOutput output) {
        return new FeatureResponse(
                output.version(),
                output.identifier(),
                output.code(),
                output.name(),
                output.description(),
                output.microserviceIdentifier(),
                output.microserviceCode(),
                output.lifecycle(),
                output.shareable(),
                output.settings(),
                output.createdAt(),
                output.updatedAt());
    }
}
