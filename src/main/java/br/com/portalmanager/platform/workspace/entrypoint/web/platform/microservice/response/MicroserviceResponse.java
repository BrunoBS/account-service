package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.response;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.MicroserviceOutput;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

public record MicroserviceResponse(
        Long version, String identifier, String code, String name, String description,
        JsonNode settings, String lifecycle, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public static MicroserviceResponse from(MicroserviceOutput output) {
        return new MicroserviceResponse(output.version(), output.identifier(), output.code(), output.name(),
                output.description(), toJsonNode(output.settings()), output.lifecycle(), output.createdAt(), output.updatedAt());
    }

    private static JsonNode toJsonNode(String settings) {
        if (settings == null || settings.isBlank()) return null;
        return JSON_MAPPER.readTree(settings);
    }
}
