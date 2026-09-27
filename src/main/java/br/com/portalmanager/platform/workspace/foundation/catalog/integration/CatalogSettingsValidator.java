package br.com.portalmanager.platform.workspace.foundation.catalog.integration;

import br.com.portalmanager.platform.library.catalog.validation.CatalogValidationResult;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class CatalogSettingsValidator {

    private static final String SETTINGS = "settings";

    private final JsonSchemaValidator jsonSchemaValidator;
    private final SchemaResolutionPort schemaResolutionPort;

    public CatalogSettingsValidator(
            JsonSchemaValidator jsonSchemaValidator,
            SchemaResolutionPort schemaResolutionPort
    ) {
        this.jsonSchemaValidator = jsonSchemaValidator;
        this.schemaResolutionPort = schemaResolutionPort;
    }

    public void validateSettings(
            String schemaTypeCode,
            JsonNode settings,
            CatalogValidationResult result
    ) {
        ValidationResult schemaResult = new ValidationResult();
        String definition = schemaResolutionPort.resolvePlatform(schemaTypeCode);
        jsonSchemaValidator.validateJson(
                definition,
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
