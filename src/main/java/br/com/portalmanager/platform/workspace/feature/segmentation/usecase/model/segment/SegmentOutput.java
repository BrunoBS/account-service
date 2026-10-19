package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment;

import java.time.LocalDateTime;

public record SegmentOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
