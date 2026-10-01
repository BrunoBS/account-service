package br.com.portalmanager.platform.workspace.core.environment.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidator;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Objects;

@Component
public class EnvironmentValidator {
    private final SchemaSettingsValidator settingsValidator;

    public EnvironmentValidator(SchemaSettingsValidator settingsValidator) {
        this.settingsValidator = settingsValidator;
    }

    public void validateForCreate(CreateEnvironmentInput input, boolean duplicate) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", EnvironmentMessageKeys.NAME_INVALID);
        } else {
            common(input.name(), input.description(), input.authorizationType(), input.sortOrder(),
                    input.settings(), duplicate, result);
        }
        reject(result);
    }

    public void validateForUpdate(UpdateEnvironmentInput input, boolean duplicate) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", EnvironmentMessageKeys.NAME_INVALID);
        } else {
            if (input.version() == null || input.version() < 0)
                result.addError("version", EnvironmentMessageKeys.VERSION_REQUIRED);
            common(input.name(), input.description(), input.authorizationType(), input.sortOrder(),
                    input.settings(), duplicate, result);
        }
        reject(result);
    }

    public void requireVersion(Long current, Long requested) {
        if (!Objects.equals(current, requested)) throw new ResourceVersionConflictException();
    }

    private void common(String name, String description, String authorization, Integer sort, JsonNode settings,
                        boolean duplicate, ValidationResult result) {
        if (name == null || name.length() < 3 || name.length() > 50)
            result.addError("name", EnvironmentMessageKeys.NAME_INVALID);
        if (duplicate) result.addError("name", EnvironmentMessageKeys.NAME_DUPLICATE);
        if (description == null || description.length() < 3 || description.length() > 250)
            result.addError("description", EnvironmentMessageKeys.DESCRIPTION_INVALID);
        if (authorization == null || Arrays.stream(AuthorizationTypeEnum.values()).noneMatch(e -> e.name().equals(authorization)))
            result.addError("authorizationType", EnvironmentMessageKeys.AUTHORIZATION_INVALID);
        if (sort != null && sort < 1) result.addError("sortOrder", EnvironmentMessageKeys.SORT_INVALID);
        if (settings != null)
        settingsValidator.validate("ENVIRONMENT", "workspace", "settings", settings, result);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
