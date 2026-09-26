package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.response;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureOutput;

import java.time.LocalDateTime;

public record FeatureResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String microserviceIdentifier,
        String microserviceCode,
        String lifecycle,
        String settings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
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
                output.settings(),
                output.createdAt(),
                output.updatedAt()
        );
    }
}
