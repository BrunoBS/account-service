package br.com.portalmanager.platform.workspace.core.application.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase.ApplicationScopeTypeService;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidator;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ApplicationValidator {
    private final ApplicationScopeTypeService scopeService;
    private final SchemaSettingsValidator settingsValidator;

    public ApplicationValidator(ApplicationScopeTypeService scopeService,
                                SchemaSettingsValidator settingsValidator) {
        this.scopeService = scopeService;
        this.settingsValidator = settingsValidator;
    }

    public void validateForCreate(String workspaceType, CreateApplicationInput input, boolean duplicate) {
        ValidationResult result = new ValidationResult();
        if (input == null) { result.addError("request", ApplicationMessageKeys.NAME_INVALID); reject(result); return; }
        common(workspaceType, input.name(), input.alias(), input.acronym(), input.applicationScope(), input.settings(), duplicate, result);
        reject(result);
    }

    public void validateForUpdate(String workspaceType, UpdateApplicationInput input, boolean duplicate) {
        ValidationResult result = new ValidationResult();
        if (input == null) { result.addError("request", ApplicationMessageKeys.NAME_INVALID); reject(result); return; }
        if (input.version() == null || input.version() < 0) result.addError("version", ApplicationMessageKeys.VERSION_REQUIRED);
        common(workspaceType, input.name(), input.alias(), input.acronym(), input.applicationScope(), input.settings(), duplicate, result);
        reject(result);
    }

    public void requireVersion(Long current, Long requested) {
        if (!Objects.equals(current, requested)) throw new ResourceVersionConflictException();
    }

    private void common(String workspaceType, String name, String alias, String acronym, String scope,
                        String settings, boolean duplicate, ValidationResult result) {
        if (!"MANAGER".equals(workspaceType))
            result.addError("workspaceIdentifier", ApplicationMessageKeys.WORKSPACE_TYPE_INVALID);
        if (name == null || name.length() < 3 || name.length() > 100)
            result.addError("name", ApplicationMessageKeys.NAME_INVALID);
        if (duplicate) result.addError("name", ApplicationMessageKeys.NAME_DUPLICATE);
        if (alias == null || alias.isBlank() || alias.length() > 100)
            result.addError("alias", ApplicationMessageKeys.ALIAS_REQUIRED);
        if (acronym == null || acronym.isBlank() || acronym.length() > 20)
            result.addError("acronym", ApplicationMessageKeys.ACRONYM_REQUIRED);
        if (scope == null || !scopeService.existsActive(scope))
            result.addError("applicationScope", ApplicationMessageKeys.SCOPE_INVALID);
        if (settings != null) {
        settingsValidator.validate("APPLICATION", "application", "settings", settings, result);
        }
    }

    private void reject(ValidationResult result) { if (result.hasErrors()) throw new ValidationException(result); }
}
