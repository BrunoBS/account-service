package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.category;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment.SegmentQueryService;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.validation.SegmentQueryValidation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryQueryService {
    private final CategoryRepository repository;
    private final SegmentQueryService segments;

    public CategoryQueryService(CategoryRepository repository, SegmentQueryService segments) {
        this.repository = repository;
        this.segments = segments;
    }

    @Transactional(readOnly = true)
    public CategoryOutput findByIdentifier(String segmentIdentifier, String identifier) {
        return repository
                .findOutputByIdentifier(segmentIdentifier, identifier)
                .orElseThrow(
                        () -> new NotFoundException(SegmentationMessageKeys.CATEGORY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<CategoryOutput> findAll(
            String segmentIdentifier, String lifecycle, Pageable pageable) {
        SegmentQueryValidation.validatePage(pageable);
        segments.findByIdentifier(segmentIdentifier);
        return repository.findPage(
                segmentIdentifier, SegmentQueryValidation.resolveLifecycle(lifecycle), pageable);
    }
}
