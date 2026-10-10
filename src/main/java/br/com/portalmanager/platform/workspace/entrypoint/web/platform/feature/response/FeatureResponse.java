package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.response;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureOutput;
import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public record FeatureResponse(
    Long version,
    String identifier,
    String code,
    String name,
    String description,
    String microserviceIdentifier,
    String microserviceCode,
    String lifecycle,
    JsonNode settings,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

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
            toJsonNode(output.settings()),
            output.createdAt(),
            output.updatedAt()
        );
    }

    private static JsonNode toJsonNode(String settings) {
        if (settings == null || settings.isBlank()) {
            return null;
        }
        return JSON_MAPPER.readTree(settings);
    }
}
