package br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context;

import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import java.time.LocalDateTime;

public record ContextOutput(
    Long version,
    String identifier,
    String code,
    String name,
    String description,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ContextOutput from(Context context) {
        return new ContextOutput(
            context.getVersion(),
            context.getIdentifier(),
            context.getCode(),
            context.getName(),
            context.getDescription(),
            context.getLifecycle().toString(),
            context.getCreatedAt(),
            context.getUpdatedAt()
        );
    }
}
