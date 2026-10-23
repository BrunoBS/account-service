package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.validation.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaMessageKeys;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class SchemaDefinitionValidationAdapter {

    private final ObjectMapper objectMapper;
    private final JsonSchemaValidator validator;

    public SchemaDefinitionValidationAdapter(
            ObjectMapper objectMapper, JsonSchemaValidator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public void requireValidSchemaSyntax(JsonNode definition, String field) {
        if (definition == null || definition.isEmpty() || !definition.isObject()) {
            throw new ValidationException(
                    new ValidationResult(field, SchemaMessageKeys.INVALID_SYNTAX));
        }
        validator.validateDefinition(definition, field);
    }

    public String toJsonString(JsonNode node, String attributeName) {
        try {
            return node != null ? objectMapper.writeValueAsString(node) : null;
        } catch (JacksonException exception) {
            throw new ValidationException(
                    new ValidationResult(
                            attributeName, SchemaMessageKeys.JSON_SERIALIZATION_INVALID));
        }
    }
}
