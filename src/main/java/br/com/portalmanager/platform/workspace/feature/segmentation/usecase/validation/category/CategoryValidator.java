package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.validation.category;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.category.Category;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CreateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.UpdateCategoryInput;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CategoryValidator {
    private final CategoryRepository repository;

    public CategoryValidator(CategoryRepository repository) {
        this.repository = repository;
    }

    public void validateForCreate(Segment segment, CreateCategoryInput input) {
        if (input == null) throw new ValidationException(SegmentationMessageKeys.REQUEST_REQUIRED);
        if (input.version() != null)
            throw new ValidationException(
                    new ValidationResult(
                            "version", SegmentationMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE));
        if (repository.existsBySegmentIdAndCode(segment.getId(), input.code()))
            throw new ConflictException(SegmentationMessageKeys.CATEGORY_CODE_DUPLICATE);
    }

    public void validateForUpdate(Category resource, UpdateCategoryInput input) {
        if (input == null) throw new ValidationException(SegmentationMessageKeys.REQUEST_REQUIRED);
        if (input.version() == null)
            throw new ValidationException(
                    new ValidationResult("version", SegmentationMessageKeys.VERSION_REQUIRED));
        if (!Objects.equals(resource.getVersion(), input.version()))
            throw new ResourceVersionConflictException();
        ValidationResult result = new ValidationResult();
        if (!resource.getCode().equals(input.code()))
            result.addError("code", SegmentationMessageKeys.CODE_IMMUTABLE);
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
