package br.com.portalmanager.platform.workspace.core.publisher.usecase.model;

import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import java.time.LocalDateTime;

public record PublisherOutput(Long version, String identifier, String code, String name, String description,
                              String scope, boolean deprecated, String settings, String lifecycle,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static PublisherOutput from(Publisher p) {
        return new PublisherOutput(p.getVersion(), p.getIdentifier(), p.getCode(), p.getName(), p.getDescription(),
                p.getScope().value(), p.isDeprecated(), p.getSettings(), p.getLifecycle().value(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
