package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class SchemaDefinitionValidator {

    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;

    public SchemaDefinitionValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    }

    public void validateJson(
            String schemaDefinition,
            JsonNode configNode,
            String attributeName,
            ValidationResult result
    ) {
        if (schemaDefinition == null || schemaDefinition.isBlank()) {
            result.addError("schema", SchemaMessageKeys.UNDEFINED);
            return;
        }
        if (configNode == null || configNode.isNull()) {
            result.addError(attributeName, SchemaMessageKeys.VALUE_REQUIRED, Map.of("0", attributeName));
            return;
        }

        Schema schema = parseSchema(schemaDefinition, result);
        if (schema != null) {
            schema.validate(configNode).forEach(error -> {
                String field = resolveField(attributeName, error);
                result.addError(
                        field,
                        SchemaMessageKeys.JSON_INVALID,
                        Map.of("0", field, "1", error.getMessage())
                );
            });
        }
    }

    public void requireValidSchemaSyntax(JsonNode schemaNode, String attributeName) {
        ValidationResult result = new ValidationResult();
        validateSchemaSyntax(schemaNode, attributeName, result);
        if (result.hasErrors()) throw new ValidationException(result);
    }

    private void validateSchemaSyntax(JsonNode schemaNode, String attributeName, ValidationResult result) {
        if (schemaNode == null || schemaNode.isEmpty() || !schemaNode.isObject()) {
            result.addError(attributeName, SchemaMessageKeys.INVALID_SYNTAX);
            return;
        }
        try {
            schemaRegistry.getSchema(schemaNode);
        } catch (Exception exception) {
            result.addError(attributeName, SchemaMessageKeys.INVALID_SYNTAX);
        }
    }

    public String toJsonString(JsonNode node, String attributeName) {
        try {
            return node != null ? objectMapper.writeValueAsString(node) : null;
        } catch (JacksonException exception) {
            throw new ValidationException(
                    new ValidationResult(attributeName, SchemaMessageKeys.JSON_SERIALIZATION_INVALID)
            );
        }
    }

    public JsonNode fromString(String json, String attributeName) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new ValidationException(
                    new ValidationResult(attributeName, SchemaMessageKeys.PERSISTED_JSON_INVALID)
            );
        }
    }

    private Schema parseSchema(String schemaDefinition, ValidationResult result) {
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaDefinition);
            return schemaRegistry.getSchema(schemaNode);
        } catch (Exception exception) {
            result.addError("schema", SchemaMessageKeys.INVALID_SYNTAX);
            return null;
        }
    }

    private String resolveField(String attributeName, ValidationMessage error) {
        String instanceLocation = error.getInstanceLocation() != null
                ? error.getInstanceLocation().toString()
                : "";

        String field = appendJsonPointer(attributeName, instanceLocation);
        String property = error.getProperty();

        if (property != null && !property.isBlank() && !fieldEndsWithProperty(field, property)) {
            field = appendProperty(field, property);
        }

        return field;
    }

    private String appendJsonPointer(String attributeName, String instanceLocation) {
        if (instanceLocation == null || instanceLocation.isBlank() || "/".equals(instanceLocation)) {
            return attributeName;
        }

        StringBuilder field = new StringBuilder(attributeName);
        for (String token : instanceLocation.split("/")) {
            if (token.isBlank()) continue;

            String decodedToken = token.replace("~1", "/").replace("~0", "~");
            if (decodedToken.chars().allMatch(Character::isDigit)) {
                field.append('[').append(decodedToken).append(']');
            } else {
                field.append('.').append(decodedToken);
            }
        }
        return field.toString();
    }

    private boolean fieldEndsWithProperty(String field, String property) {
        return field.equals(property)
                || field.endsWith("." + property)
                || field.endsWith("[" + property + "]");
    }

    private String appendProperty(String field, String property) {
        return property.chars().allMatch(Character::isDigit)
                ? field + "[" + property + "]"
                : field + "." + property;
    }
}
