package br.com.portalmanager.platform.workspace.foundation.catalog.support;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaValidator;
import br.com.portalmanager.platform.catalog.validation.CatalogValidationResult;
import br.com.portalmanager.platform.messaging.validation.ValidationResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class CatalogSchemaValidationSupport {

    private final SchemaValidator schemaValidator;

    public CatalogSchemaValidationSupport(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
    }

    public void validateSettings(JsonNode settings, CatalogValidationResult result) {
        ValidationResult schemaResult = new ValidationResult();
        schemaValidator.validateJson(
                SchemaDefaults.DEFAULT_JSON_SCHEMA,
                settings,
                "settings",
                schemaResult
        );

        schemaResult.getDetails().forEach(detail ->
                result.addError(detail.field(), detail.messageKey(), detail.parameters())
        );
    }

    public JsonNode fromString(String json) {
        return schemaValidator.fromString(json);
    }
}
