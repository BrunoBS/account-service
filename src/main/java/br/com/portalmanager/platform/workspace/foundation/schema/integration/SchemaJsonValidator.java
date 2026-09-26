package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class SchemaJsonValidator implements JsonSchemaValidator {

    private final SchemaValidator schemaValidator;

    public SchemaJsonValidator(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
    }

    @Override
    public void validateJson(
            String schemaDefinition,
            JsonNode jsonNode,
            String attributeName,
            ValidationResult result
    ) {
        schemaValidator.validateJson(schemaDefinition, jsonNode, attributeName, result);
    }

    @Override
    public JsonNode fromString(String json) {
        return schemaValidator.fromString(json);
    }
}
