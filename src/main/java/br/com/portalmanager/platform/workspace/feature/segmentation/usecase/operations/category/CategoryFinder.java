package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.category;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.category.Category;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment.SegmentFinder;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;

@Component
public class CategoryFinder {
    private final CategoryRepository repository;
    private final EntityManager entityManager;
    private final SegmentFinder segments;

    public CategoryFinder(
            CategoryRepository repository, EntityManager entityManager, SegmentFinder segments) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.segments = segments;
    }

    public Category findForUpdate(String segmentIdentifier, String identifier) {
        // Lock/reload the parent first so availability uses its current state, including cached
        // transactions.
        segments.findForUpdate(segmentIdentifier);
        Long internalId =
                repository
                        .findInternalIdByIdentifierForUpdate(segmentIdentifier, identifier)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                SegmentationMessageKeys.CATEGORY_NOT_FOUND));
        // Locking reads observe committed state even after a prior repeatable-read snapshot.
        entityManager.detach(entityManager.getReference(Category.class, internalId));
        return repository
                .findByIdentifierForUpdate(segmentIdentifier, identifier)
                .orElseThrow(
                        () -> new NotFoundException(SegmentationMessageKeys.CATEGORY_NOT_FOUND));
    }
}
