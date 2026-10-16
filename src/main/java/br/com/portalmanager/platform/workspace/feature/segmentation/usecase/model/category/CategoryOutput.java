package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category;

import br.com.portalmanager.platform.workspace.feature.segmentation.domain.category.Category;

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
        LocalDateTime updatedAt) {
    public static CategoryOutput from(Category category) {
        return new CategoryOutput(
                category.getVersion(),
                category.getIdentifier(),
                category.getCode(),
                category.getName(),
                category.getLifecycle().value(),
                category.getSegment().getIdentifier(),
                category.isAvailable(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
