package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

import java.util.regex.Pattern;

final class PlatformValidation {
    private static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");
    private static final String PREFIX = "platform.";

    private PlatformValidation() {}

    static void validate(String code, boolean requireCode, String name, String description, boolean codeDuplicate,
                         boolean nameDuplicate, String serviceIdentifier, boolean requireService,
                         String settings, boolean requireSettings) {
        ValidationResult result = new ValidationResult();
        if ((requireCode && code == null) || (code != null &&
                (code.length() > 50 || !CODE.matcher(code).matches()))) {
            result.addError("code", PREFIX + "code.invalid");
        } else if (codeDuplicate) {
            result.addError("code", PREFIX + "code.duplicate");
        }
        if (name == null || name.isBlank() || name.length() > 100) {
            result.addError("name", PREFIX + "name.invalid");
        } else if (nameDuplicate) {
            result.addError("name", PREFIX + "name.duplicate");
        }
        if (description != null && description.length() > 500) {
            result.addError("description", PREFIX + "description.invalid");
        }
        if (requireService && (serviceIdentifier == null || serviceIdentifier.isBlank())) {
            result.addError("serviceIdentifier", PREFIX + "service.required");
        }
        if (requireSettings && (settings == null || settings.isBlank())) {
            result.addError("settings", PREFIX + "settings.required");
        }
        if (result.hasErrors()) throw new ValidationException(result);
    }

    static void requireInput(Object input) {
        if (input == null) {
            reject("request", "request.required");
        }
    }

    static void reject(String field, String key) {
        throw new ValidationException(new ValidationResult(field, PREFIX + key));
    }
}
