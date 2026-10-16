package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.validation.segment;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.SegmentationMessageKeys;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.CreateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.UpdateSegmentInput;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SegmentValidator {
    private final SegmentRepository repository;

    public SegmentValidator(SegmentRepository repository) {
        this.repository = repository;
    }

    public void validateForCreate(CreateSegmentInput input) {
        if (input == null) throw new ValidationException(SegmentationMessageKeys.REQUEST_REQUIRED);
        if (input.version() != null)
            throw new ValidationException(
                    new ValidationResult(
                            "version", SegmentationMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE));
        if (repository.existsByCode(input.code()))
            throw new ConflictException(SegmentationMessageKeys.CODE_DUPLICATE);
    }

    public void validateForUpdate(Segment resource, UpdateSegmentInput input) {
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
