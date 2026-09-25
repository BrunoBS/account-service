package br.com.itau.portalmanager.workspace.entrypoint.web.platform.feature.response;

import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.FeatureOutput;

import java.time.LocalDateTime;

public record FeatureResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String serviceIdentifier,
        String serviceCode,
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
                output.serviceIdentifier(),
                output.serviceCode(),
                output.lifecycle(),
                output.settings(),
                output.createdAt(),
                output.updatedAt()
        );
    }
}
