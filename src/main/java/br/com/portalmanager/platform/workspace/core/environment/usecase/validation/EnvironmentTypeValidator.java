package br.com.portalmanager.platform.workspace.core.environment.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTypeValidator {
    public void validate(EnvironmentTypeInput input, boolean creating) {
        if (input == null || input.code() == null || input.code().isBlank() || input.code().length() > 50
                || input.name() == null || input.name().isBlank() || input.name().length() > 100
                || input.description() == null || input.description().isBlank() || input.description().length() > 250
                || input.rootAllowed() == null || input.workspaceRequired() == null
                || input.displayOrder() == null || input.displayOrder() < 0
                || (!creating && input.version() == null))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_INVALID);
    }
}
