package br.com.itau.portalmanager.workspace.feature.platform.usecase.model;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;

import java.time.LocalDateTime;

public record FeatureOutput(
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
    public static FeatureOutput from(Feature feature) {
        return new FeatureOutput(
                feature.getVersion(),
                feature.getIdentifier(),
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                feature.getService().getIdentifier(),
                feature.getService().getCode(),
                feature.getLifecycle().toString(),
                feature.getSettings(),
                feature.getCreatedAt(),
                feature.getUpdatedAt()
        );
    }
}
