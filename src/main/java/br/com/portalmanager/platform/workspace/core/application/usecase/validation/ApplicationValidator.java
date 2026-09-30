package br.com.portalmanager.platform.workspace.core.application.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase.ApplicationScopeTypeService;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ApplicationValidator {

    private final ApplicationScopeTypeService scopeService;

    public ApplicationValidator(ApplicationScopeTypeService scopeService) {
        this.scopeService = scopeService;
    }

    public void validateForCreate(String workspaceType, CreateApplicationInput input, boolean duplicate) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", ApplicationMessageKeys.NAME_INVALID);
            reject(result);
            return;
        }

        business(workspaceType, input.applicationScope(), duplicate, result);
        reject(result);
    }

    public void validateForUpdate(String workspaceType, UpdateApplicationInput input, boolean duplicate) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", ApplicationMessageKeys.NAME_INVALID);
            reject(result);
            return;
        }

        if (input.version() == null) {
            result.addError("version", ApplicationMessageKeys.VERSION_REQUIRED);
        }
        business(workspaceType, input.applicationScope(), duplicate, result);
        reject(result);
    }

    public void requireVersion(Long current, Long requested) {
        if (!Objects.equals(current, requested)) throw new ResourceVersionConflictException();
    }

    private void business(String workspaceType, String scope, boolean duplicate, ValidationResult result) {
        if (!"MANAGER".equals(workspaceType)) {
            result.addError("workspaceIdentifier", ApplicationMessageKeys.WORKSPACE_TYPE_INVALID);
        }
        if (duplicate) {
            result.addError("name", ApplicationMessageKeys.NAME_DUPLICATE);
        }
        if (scope != null && !scopeService.existsActive(scope)) {
            result.addError("applicationScope", ApplicationMessageKeys.SCOPE_INVALID);
        }
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
