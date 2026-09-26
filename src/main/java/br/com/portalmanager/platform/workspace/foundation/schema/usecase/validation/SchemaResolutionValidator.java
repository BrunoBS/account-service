package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;

public class SchemaResolutionValidator {

    public String normalizeRequired(String value) {
        if (value == null || value.isBlank()) throw error("schema", SchemaMessageKeys.RESOLUTION_NOT_FOUND);
        return value.trim();
    }

    public void requireActive(Schema schema, boolean typeActive) {
        if (!schema.isActive() || !typeActive) throw error("schema", SchemaMessageKeys.INACTIVE);
    }

    public ValidationException schemaNotFound() {
        return error("schema", SchemaMessageKeys.RESOLUTION_NOT_FOUND);
    }

    public ValidationException typeNotFound() {
        return error("schemaType", SchemaMessageKeys.RESOLUTION_NOT_FOUND);
    }

    public ValidationException versionNotFound() {
        return error("schemaVersion", SchemaMessageKeys.PUBLISHED_NOT_FOUND);
    }

    private ValidationException error(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
