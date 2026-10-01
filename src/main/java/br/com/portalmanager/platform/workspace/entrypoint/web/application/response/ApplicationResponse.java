package br.com.portalmanager.platform.workspace.entrypoint.web.application.response;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationResponse(Long version, String identifier, String workspaceIdentifier, String name,
                                  String alias, String acronym, String applicationScope, String authorizerGroup,
                                  JsonNode settings, String lifecycle,
                                  LocalDateTime createdAt, LocalDateTime updatedAt, List<String> tags) {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public static ApplicationResponse from(ApplicationOutput value) {
        return new ApplicationResponse(value.version(), value.identifier(), value.workspaceIdentifier(), value.name(),
                value.alias(), value.acronym(), value.applicationScope(), value.authorizerGroup(), toJsonNode(value.settings()),
                value.lifecycle(), value.createdAt(), value.updatedAt(), value.tags());
    }

    private static JsonNode toJsonNode(JsonNode settings) {
        if (settings == null || settings.isBlank()) {
            return null;
        }
        return JSON_MAPPER.readTree(settings);
    }
}
