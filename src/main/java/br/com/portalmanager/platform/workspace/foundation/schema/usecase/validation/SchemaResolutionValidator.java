package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import org.springframework.stereotype.Component;

@Component
public class SchemaResolutionValidator {

    public ValidationException typeNotFound() {
        return error("schemaType", SchemaMessageKeys.RESOLUTION_NOT_FOUND);
    }

    public ValidationException defaultNotFound() {
        return error("schema", SchemaMessageKeys.DEFAULT_NOT_FOUND);
    }

    private ValidationException error(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
