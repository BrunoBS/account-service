package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;

import java.time.LocalDateTime;

public record FeatureGroupOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static FeatureGroupOutput from(FeatureGroup featureGroupEntity) {
        return new FeatureGroupOutput(
                featureGroupEntity.getVersion(),
                featureGroupEntity.getIdentifier(),
                featureGroupEntity.getCode(),
                featureGroupEntity.getName(),
                featureGroupEntity.getDescription(),
                featureGroupEntity.getLifecycle().toString(),
                featureGroupEntity.getCreatedAt(),
                featureGroupEntity.getUpdatedAt());
    }
}
