package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.category;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.category.Category;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CreateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.UpdateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment.SegmentFinder;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.validation.category.CategoryValidator;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.mapping.category.CategoryMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CategoryCommandService {
    private final CategoryRepository repository;
    private final CategoryFinder finder;
    private final CategoryValidator validator;
    private final SegmentFinder segments;
    private final CategoryMapper mapper;

    public CategoryCommandService(
            CategoryRepository repository,
            CategoryFinder finder,
            CategoryValidator validator,
            SegmentFinder segments,
            CategoryMapper mapper) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.segments = segments;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "CATEGORY_CREATED", resourceType = "CATEGORY")
    public CategoryOutput create(CreateCategoryInput input) {
        if (input == null) throw new ValidationException(SegmentationMessageKeys.REQUEST_REQUIRED);
        Segment segment = segments.findForUpdate(input.segmentIdentifier());
        segment.requireActiveForCategoryCreation();
        validator.validateForCreate(segment, input);
        Category resource =
                repository.saveAndFlush(
                        mapper.toDomain(segment, input, LocalDateTime.now()));
        return mapper.toOutput(resource);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "CATEGORY_UPDATED", resourceType = "CATEGORY")
    public CategoryOutput update(
            String segmentIdentifier, String identifier, UpdateCategoryInput input) {
        Category resource = finder.findForUpdate(segmentIdentifier, identifier);
        validator.validateForUpdate(resource, input);
        resource.update(input.name(), LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(resource));
    }

    @Transactional
    @Auditable(
            action = AuditAction.ACTIVATE,
            event = "CATEGORY_ACTIVATED",
            resourceType = "CATEGORY")
    public CategoryOutput activate(String segmentIdentifier, String identifier) {
        Category resource = finder.findForUpdate(segmentIdentifier, identifier);
        resource.activate(LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(resource));
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "CATEGORY_DEACTIVATED",
            resourceType = "CATEGORY")
    public CategoryOutput inactivate(String segmentIdentifier, String identifier) {
        Category resource = finder.findForUpdate(segmentIdentifier, identifier);
        resource.inactivate(LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(resource));
    }
}
