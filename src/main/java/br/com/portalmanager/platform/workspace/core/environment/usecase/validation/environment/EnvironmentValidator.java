package br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environment;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.UpdateEnvironmentInput;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentValidator {

    private final EnvironmentRepository repository;

    public EnvironmentValidator(EnvironmentRepository repository) {
        this.repository = repository;
    }

    public void validateForCreate(CreateEnvironmentInput input) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", EnvironmentMessageKeys.REQUEST_INVALID);
        } else if (input.version() != null) {
            result.addError("version", EnvironmentMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
        } else if (input.environmentTypeCode() == null || input.environmentTypeCode().isBlank()) {
            result.addError("environmentTypeCode", EnvironmentMessageKeys.TYPE_CODE_INVALID);
        }
        reject(result);
    }

    public void validateForUpdate(UpdateEnvironmentInput input) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", EnvironmentMessageKeys.REQUEST_INVALID);
        } else {
            if (input.version() == null) result.addError("version", EnvironmentMessageKeys.VERSION_REQUIRED);
            if (input.environmentTypeCode() == null || input.environmentTypeCode().isBlank()) result.addError(
                "environmentTypeCode", EnvironmentMessageKeys.TYPE_CODE_INVALID
            );
            if (input.parentIdentifier() != null) result.addError(
                "parentIdentifier", EnvironmentMessageKeys.PARENT_UPDATE_NOT_ALLOWED
            );
        }
        reject(result);
    }

    public void validateNameAvailable(String name, Long workspaceId, Long parentId, Long excludedId) {
        if (name == null || !repository.existsSibling(name, workspaceId, parentId, excludedId)) return;
        ValidationResult result = new ValidationResult();
        result.addError("name", EnvironmentMessageKeys.NAME_DUPLICATE);
        reject(result);
    }

    public void requireVersion(Long currentVersion, Long requestedVersion) {
        if (!Objects.equals(currentVersion, requestedVersion)) throw new ResourceVersionConflictException();
    }

    public void validateForDelete(Environment environment) {
        environment.requireDeletable();
        if (repository.findChildForEnvironmentValidation(environment.getId()).isPresent()) {
            throw new ValidationException(EnvironmentMessageKeys.HAS_CHILDREN);
        }
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
