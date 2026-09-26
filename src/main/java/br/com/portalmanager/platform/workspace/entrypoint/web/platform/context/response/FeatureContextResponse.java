package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;

import java.time.LocalDateTime;

public record FeatureContextResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FeatureContextResponse from(FeatureContextOutput output) {
        return new FeatureContextResponse(
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
