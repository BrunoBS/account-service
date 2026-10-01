package br.com.portalmanager.platform.workspace.entrypoint.web.publisher.response;

import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.PublisherOutput;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record PublisherResponse(Long version, String identifier, String code, String name, String description,
                                String scope, boolean deprecated, JsonNode settings, String lifecycle,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static PublisherResponse from(PublisherOutput output) {
        return new PublisherResponse(output.version(), output.identifier(), output.code(), output.name(),
                output.description(), output.scope(), output.deprecated(), output.settings(),
                output.lifecycle(), output.createdAt(), output.updatedAt());
    }

}
