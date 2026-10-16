package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment;

import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;

import java.time.LocalDateTime;

public record SegmentOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static SegmentOutput from(Segment segment) {
        return new SegmentOutput(
                segment.getVersion(),
                segment.getIdentifier(),
                segment.getCode(),
                segment.getName(),
                segment.getLifecycle().value(),
                segment.getCreatedAt(),
                segment.getUpdatedAt());
    }
}
