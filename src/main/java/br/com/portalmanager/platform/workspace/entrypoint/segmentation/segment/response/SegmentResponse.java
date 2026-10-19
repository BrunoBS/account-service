package br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.response;

import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.SegmentOutput;

import java.time.LocalDateTime;

public record SegmentResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static SegmentResponse from(SegmentOutput output) {
        return new SegmentResponse(
                output.version(),
                output.identifier(),
                output.code(),
                output.name(),
                output.lifecycle(),
                output.createdAt(),
                output.updatedAt());
    }
}
