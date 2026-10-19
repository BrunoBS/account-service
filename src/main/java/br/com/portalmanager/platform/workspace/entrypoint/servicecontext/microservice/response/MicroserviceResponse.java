package br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.response;

import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceOutput;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record MicroserviceResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        JsonNode settings,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static MicroserviceResponse from(MicroserviceOutput output) {
        return new MicroserviceResponse(
                output.version(),
                output.identifier(),
                output.code(),
                output.name(),
                output.description(),
                output.settings(),
                output.lifecycle(),
                output.createdAt(),
                output.updatedAt());
    }
}
