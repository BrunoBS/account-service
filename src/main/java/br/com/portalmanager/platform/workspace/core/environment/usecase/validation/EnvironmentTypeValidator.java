package br.com.portalmanager.platform.workspace.core.environment.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTypeValidator {
    public void validateForCreate(EnvironmentTypeInput input, boolean duplicateCode) {
        ValidationResult result = fields(input, false);
        if (duplicateCode) result.addError("code", EnvironmentMessageKeys.TYPE_DUPLICATE);
        reject(result);
    }

    public void validateForUpdate(EnvironmentTypeInput input) {
        reject(fields(input, true));
    }

    public void validateUpdateConflicts(boolean codeChanged, boolean workspaceScopeInUse, boolean rootInUse) {
        ValidationResult result = new ValidationResult();
        if (codeChanged) result.addError("code", EnvironmentMessageKeys.TYPE_CODE_IMMUTABLE);
        if (workspaceScopeInUse) result.addError("workspaceRequired", EnvironmentMessageKeys.TYPE_IN_USE);
        if (rootInUse) result.addError("rootAllowed", EnvironmentMessageKeys.TYPE_IN_USE);
        reject(result);
    }

    public void validateForInactivate(boolean inUse) {
        ValidationResult result = new ValidationResult();
        if (inUse) result.addError("identifier", EnvironmentMessageKeys.TYPE_IN_USE);
        reject(result);
    }

    public void validateForDelete(boolean inactive, boolean inUse) {
        ValidationResult result = new ValidationResult();
        if (!inactive) result.addError("lifecycle", EnvironmentMessageKeys.TYPE_DELETE_REQUIRES_INACTIVE);
        if (inUse) result.addError("identifier", EnvironmentMessageKeys.TYPE_IN_USE);
        reject(result);
    }

    private ValidationResult fields(EnvironmentTypeInput input, boolean update) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", EnvironmentMessageKeys.TYPE_REQUEST_INVALID);
            return result;
        }
        if (input.code() == null || input.code().isBlank() || input.code().length() > 50)
            result.addError("code", EnvironmentMessageKeys.TYPE_CODE_INVALID);
        if (input.name() == null || input.name().isBlank() || input.name().length() > 100)
            result.addError("name", EnvironmentMessageKeys.TYPE_NAME_INVALID);
        if (input.description() == null || input.description().isBlank() || input.description().length() > 250)
            result.addError("description", EnvironmentMessageKeys.TYPE_DESCRIPTION_INVALID);
        if (input.rootAllowed() == null)
            result.addError("rootAllowed", EnvironmentMessageKeys.TYPE_ROOT_ALLOWED_REQUIRED);
        if (input.workspaceRequired() == null)
            result.addError("workspaceRequired", EnvironmentMessageKeys.TYPE_WORKSPACE_REQUIRED_REQUIRED);
        if (input.displayOrder() == null || input.displayOrder() < 0)
            result.addError("displayOrder", EnvironmentMessageKeys.TYPE_DISPLAY_ORDER_INVALID);
        if (update && (input.version() == null || input.version() < 0))
            result.addError("version", EnvironmentMessageKeys.TYPE_VERSION_REQUIRED);
        return result;
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
