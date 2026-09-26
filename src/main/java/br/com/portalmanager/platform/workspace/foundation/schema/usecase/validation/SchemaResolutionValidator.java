package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

public class SchemaResolutionValidator {
    private static final String NOT_FOUND = "workspace-service.schema.resolution.not-found";
    private static final String PUBLISHED_NOT_FOUND = "workspace-service.schema.published.not-found";
    private static final String INACTIVE = "workspace-service.schema.inactive";

    public String normalizeRequired(String value) {
        if (value == null || value.isBlank()) throw error("schema", NOT_FOUND);
        return value.trim();
    }

    public void requireActive(Schema schema, boolean typeActive) {
        if (!schema.isActive() || !typeActive) throw error("schema", INACTIVE);
    }

    public ValidationException schemaNotFound() { return error("schema", NOT_FOUND); }
    public ValidationException typeNotFound() { return error("schemaType", NOT_FOUND); }
    public ValidationException versionNotFound() { return error("schemaVersion", PUBLISHED_NOT_FOUND); }

    private ValidationException error(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
