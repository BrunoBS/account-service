package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;

@Component
public class SegmentFinder {
    private final SegmentRepository repository;
    private final EntityManager entityManager;

    public SegmentFinder(SegmentRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public Segment findForUpdate(String identifier) {
        Long internalId =
                repository
                        .findInternalIdByIdentifierForUpdate(identifier)
                        .orElseThrow(
                                () -> new NotFoundException(SegmentationMessageKeys.NOT_FOUND));
        // Locking reads observe committed state even after a prior repeatable-read snapshot.
        entityManager.detach(entityManager.getReference(Segment.class, internalId));
        return repository
                .findByIdentifierForUpdate(identifier)
                .orElseThrow(() -> new NotFoundException(SegmentationMessageKeys.NOT_FOUND));
    }
}
