package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidationPort;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class JsonSchemaValidationAdapter implements JsonSchemaValidationPort {

    private final SchemaDefinitionValidationAdapter definitionValidator;

    public JsonSchemaValidationAdapter(SchemaDefinitionValidationAdapter definitionValidator) {
        this.definitionValidator = definitionValidator;
    }

    @Override
    public void validateJson(
        String schemaDefinition,
        JsonNode jsonNode,
        String attributeName,
        ValidationResult result
    ) {
        definitionValidator.validateJson(schemaDefinition, jsonNode, attributeName, result);
    }

    @Override
    public JsonNode fromString(String json, String attributeName) {
        return definitionValidator.fromString(json, attributeName);
    }
}
