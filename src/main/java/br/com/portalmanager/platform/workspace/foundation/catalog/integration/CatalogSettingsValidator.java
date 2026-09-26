package br.com.portalmanager.platform.workspace.foundation.catalog.integration;

import br.com.portalmanager.platform.library.catalog.validation.CatalogValidationResult;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaDefaults;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class CatalogSettingsValidator {

    private static final String SETTINGS = "settings";

    private final JsonSchemaValidator jsonSchemaValidator;

    public CatalogSettingsValidator(JsonSchemaValidator jsonSchemaValidator) {
        this.jsonSchemaValidator = jsonSchemaValidator;
    }

    public void validateSettings(JsonNode settings, CatalogValidationResult result) {
        ValidationResult schemaResult = new ValidationResult();
        jsonSchemaValidator.validateJson(
                SchemaDefaults.DEFAULT_JSON_SCHEMA,
                settings,
                SETTINGS,
                schemaResult
        );

        schemaResult.getDetails().forEach(detail ->
                result.addError(detail.field(), detail.messageKey(), detail.parameters())
        );
    }

    public JsonNode fromString(String json) {
        return jsonSchemaValidator.fromString(json, SETTINGS);
    }
}
