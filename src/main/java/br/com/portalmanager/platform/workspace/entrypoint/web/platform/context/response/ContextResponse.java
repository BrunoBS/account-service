package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.ContextOutput;
import java.time.LocalDateTime;

public record ContextResponse(
    Long version,
    String identifier,
    String code,
    String name,
    String description,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ContextResponse from(ContextOutput output) {
        return new ContextResponse(
            output.version(),
            output.identifier(),
            output.code(),
            output.name(),
            output.description(),
            output.lifecycle(),
            output.createdAt(),
            output.updatedAt()
        );
    }
}
