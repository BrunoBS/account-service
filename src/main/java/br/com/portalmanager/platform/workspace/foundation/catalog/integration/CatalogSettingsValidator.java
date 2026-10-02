package br.com.portalmanager.platform.workspace.foundation.catalog.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class CatalogSettingsValidator {

    private static final String SETTINGS = "settings";

    private final JsonSchemaValidator jsonSchemaValidator;
    private final SchemaSettingsValidator settingsValidator;

    public CatalogSettingsValidator(
            JsonSchemaValidator jsonSchemaValidator,
            SchemaSettingsValidator settingsValidator
    ) {
        this.jsonSchemaValidator = jsonSchemaValidator;
        this.settingsValidator = settingsValidator;
    }

    public void validateSettings(
            String catalogCode,
            JsonNode settings,
            ValidationResult result
    ) {
        ValidationResult schemaResult = new ValidationResult();
        settingsValidator.validate("CATALOG", catalogCode, SETTINGS, settings, schemaResult);
        result.merge(schemaResult);
    }

    public JsonNode fromString(String json) {
        return jsonSchemaValidator.fromString(json, SETTINGS);
    }
}
