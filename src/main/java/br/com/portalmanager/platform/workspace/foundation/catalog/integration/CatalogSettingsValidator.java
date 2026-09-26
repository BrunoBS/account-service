package br.com.portalmanager.platform.workspace.foundation.catalog.integration;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaValidator;
import br.com.portalmanager.platform.library.catalog.validation.CatalogValidationResult;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class CatalogSettingsValidator {

    private final SchemaValidator schemaValidator;

    public CatalogSettingsValidator(SchemaValidator schemaValidator) {
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
