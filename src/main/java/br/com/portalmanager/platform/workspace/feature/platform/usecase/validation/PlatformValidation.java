package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;

import java.util.regex.Pattern;

final class PlatformValidation {
    private static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");

    private PlatformValidation() {
    }

    static void validate(String code, boolean requireCode, String name, String description, boolean codeDuplicate,
                         boolean nameDuplicate, String microserviceIdentifier, boolean requireMicroservice,
                         String settings, boolean requireSettings) {
        ValidationResult result = new ValidationResult();
        if ((requireCode && code == null) || (code != null &&
                (code.length() > 50 || !CODE.matcher(code).matches()))) {
            result.addError("code", PlatformMessageKeys.CODE_INVALID);
        } else if (codeDuplicate) {
            result.addError("code", PlatformMessageKeys.CODE_DUPLICATE);
        }
        if (name == null || name.isBlank() || name.length() > 100) {
            result.addError("name", PlatformMessageKeys.NAME_INVALID);
        } else if (nameDuplicate) {
            result.addError("name", PlatformMessageKeys.NAME_DUPLICATE);
        }
        if (description != null && description.length() > 500) {
            result.addError("description", PlatformMessageKeys.DESCRIPTION_INVALID);
        }
        if (requireMicroservice && (microserviceIdentifier == null || microserviceIdentifier.isBlank())) {
            result.addError("microserviceIdentifier", PlatformMessageKeys.MICROSERVICE_REQUIRED);
        }
        if (requireSettings && (settings == null || settings.isBlank())) {
            result.addError("settings", PlatformMessageKeys.SETTINGS_REQUIRED);
        }
        if (result.hasErrors()) throw new ValidationException(result);
    }

    static void requireInput(Object input) {
        if (input == null) {
            reject("request", PlatformMessageKeys.REQUEST_REQUIRED);
        }
    }

    static void reject(String field, String key) {
        throw new ValidationException(new ValidationResult(field, key));
    }
}
