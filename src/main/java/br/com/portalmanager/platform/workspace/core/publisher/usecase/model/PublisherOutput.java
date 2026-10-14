package br.com.portalmanager.platform.workspace.core.publisher.usecase.model;

import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record PublisherOutput(
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
    public static PublisherOutput from(Publisher publisher) {
        return new PublisherOutput(
            publisher.getVersion(),
            publisher.getIdentifier(),
            publisher.getCode(),
            publisher.getName(),
            publisher.getDescription(),
            publisher.getScope().value(),
            publisher.isDeprecated(),
            publisher.getSettings(),
            publisher.getLifecycle().value(),
            publisher.getCreatedAt(),
            publisher.getUpdatedAt()
        );
    }
}
