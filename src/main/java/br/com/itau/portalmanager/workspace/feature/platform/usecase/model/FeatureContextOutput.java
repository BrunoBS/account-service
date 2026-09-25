package br.com.itau.portalmanager.workspace.feature.platform.usecase.model;

import br.com.itau.portalmanager.workspace.feature.platform.domain.FeatureContext;

import java.time.LocalDateTime;

public record FeatureContextOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FeatureContextOutput from(FeatureContext context) {
        return new FeatureContextOutput(
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
