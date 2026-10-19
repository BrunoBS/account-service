package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup;

import java.time.LocalDateTime;

public record FeatureGroupOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
