package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category;

import java.time.LocalDateTime;

public record CategoryOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String lifecycle,
        String segmentIdentifier,
        boolean available,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
