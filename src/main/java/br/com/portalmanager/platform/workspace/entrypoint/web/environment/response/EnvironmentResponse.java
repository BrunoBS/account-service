package br.com.portalmanager.platform.workspace.entrypoint.web.environment.response;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

public record EnvironmentResponse(Long version, String identifier, String workspaceIdentifier, String name,
                                  String description, String authorizationType, String environmentType, String parentIdentifier,
                                  Integer sortOrder, String authorizerGroup, JsonNode settings, String lifecycle,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public static EnvironmentResponse from(EnvironmentOutput output) {
        return new EnvironmentResponse(output.version(), output.identifier(), output.workspaceIdentifier(),
                output.name(), output.description(), output.authorizationType(), output.environmentType(), output.parentIdentifier(),
                output.sortOrder(), output.authorizerGroup(), toJsonNode(output.settings()), output.lifecycle(),
                output.createdAt(), output.updatedAt());
    }

    private static JsonNode toJsonNode(String settings) {
        if (settings == null || settings.isBlank()) {
            return null;
        }
        return JSON_MAPPER.readTree(settings);
    }
}
