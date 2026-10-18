package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;

public final class ServiceContextValidation {

    private ServiceContextValidation() {}

    public static void requireInput(Object input) {
        if (input == null) {
            reject("request", ServiceContextMessageKeys.REQUEST_REQUIRED);
        }
    }

    public static void reject(String field, String key) {
        throw new ValidationException(new ValidationResult(field, key));
    }
}
