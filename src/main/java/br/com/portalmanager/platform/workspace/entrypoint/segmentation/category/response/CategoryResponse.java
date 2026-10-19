package br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.response;

import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String lifecycle,
        String segmentIdentifier,
        boolean available,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static CategoryResponse from(CategoryOutput output) {
        return new CategoryResponse(
                output.version(),
                output.identifier(),
                output.code(),
                output.name(),
                output.lifecycle(),
                output.segmentIdentifier(),
                output.available(),
                output.createdAt(),
                output.updatedAt());
    }
}
