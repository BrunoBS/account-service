package br.com.portalmanager.platform.workspace.foundation.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import tools.jackson.databind.JsonNode;

public interface JsonSchemaValidator {
    void validateJson(String schemaDefinition, JsonNode jsonNode, String attributeName, ValidationResult result);

    JsonNode fromString(String json, String attributeName);
}
