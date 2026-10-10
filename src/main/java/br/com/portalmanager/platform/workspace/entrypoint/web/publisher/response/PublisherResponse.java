package br.com.portalmanager.platform.workspace.entrypoint.web.publisher.response;

import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.PublisherOutput;
import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public record PublisherResponse(
    Long version,
    String identifier,
    String code,
    String name,
    String description,
    String scope,
    boolean deprecated,
    JsonNode settings,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public static PublisherResponse from(PublisherOutput output) {
        return new PublisherResponse(
            output.version(),
            output.identifier(),
            output.code(),
            output.name(),
            output.description(),
            output.scope(),
            output.deprecated(),
            toJsonNode(output.settings()),
            output.lifecycle(),
            output.createdAt(),
            output.updatedAt()
        );
    }

    private static JsonNode toJsonNode(String settings) {
        if (settings == null || settings.isBlank()) return null;
        return JSON_MAPPER.readTree(settings);
    }
}
