package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Feature;
import java.time.LocalDateTime;

public record FeatureOutput(
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
    public static FeatureOutput from(Feature feature) {
        return new FeatureOutput(
            feature.getVersion(),
            feature.getIdentifier(),
            feature.getCode(),
            feature.getName(),
            feature.getDescription(),
            feature.getMicroservice().getIdentifier(),
            feature.getMicroservice().getCode(),
            feature.getLifecycle().toString(),
            feature.getSettings(),
            feature.getCreatedAt(),
            feature.getUpdatedAt()
        );
    }
}
