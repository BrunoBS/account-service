package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;

public final class PlatformValidation {

    private PlatformValidation() {}

    public static void requireInput(Object input) {
        if (input == null) {
            reject("request", PlatformMessageKeys.REQUEST_REQUIRED);
        }
    }

    public static void reject(String field, String key) {
        throw new ValidationException(new ValidationResult(field, key));
    }
}
