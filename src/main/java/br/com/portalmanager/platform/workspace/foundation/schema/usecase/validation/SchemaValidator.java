package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SchemaValidator {

    private static final String PREFIX = "workspace-service.";

    private static final String UNDEFINED = PREFIX + "schema.undefined";
    private static final String INVALID_SYNTAX = PREFIX + "schema.invalid.syntax";
    private static final String VALUE_REQUIRED = PREFIX + "schema.value.required";
    private static final String JSON_INVALID = PREFIX + "schema.json.invalid.for.schema";
    private static final String PERSISTED_JSON_INVALID = PREFIX + "schema.persisted.json.invalid";
    private static final String JSON_SERIALIZATION_INVALID = PREFIX + "schema.json.serialization.invalid";

    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;
    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    }

    public void validateJson(
            String schemaDefinition,
            JsonNode configNode,
            String attributeName,
            ValidationResult result
    ) {
        String field = attributeName == null ? "settings" : attributeName;

        if (schemaDefinition == null || schemaDefinition.isBlank()) {
            result.addError("schema", UNDEFINED);
            return;
        }
        if (configNode == null || configNode.isNull()) {
            result.addError(field, VALUE_REQUIRED, Map.of("0", field));
            return;
        }

        Schema schema = parseSchema(schemaDefinition, result);
        if (schema != null) {
            schema.validate(configNode).forEach(error ->
                    result.addError(
                            field,
                            JSON_INVALID,
                            Map.of("0", field, "1", error.getMessage())
                    )
            );
        }
    }

    public void requireValidJson(String schemaDefinition, JsonNode configNode, String attributeName) {
        ValidationResult result = new ValidationResult();
        validateJson(schemaDefinition, configNode, attributeName, result);
        if (result.hasErrors()) throw new ValidationException(result);
    }

    public void requireValidSchemaSyntax(JsonNode schemaNode) {
        ValidationResult result = new ValidationResult();
        validateSchemaSyntax(schemaNode, result);
        if (result.hasErrors()) throw new ValidationException(result);
    }

    public void validateSchemaSyntax(JsonNode schemaNode, ValidationResult result) {
        if (schemaNode == null || schemaNode.isEmpty() || !schemaNode.isObject()) {
            result.addError("jsonSchema", INVALID_SYNTAX);
            return;
        }
        try {
            schemaRegistry.getSchema(schemaNode);
        } catch (Exception exception) {
            result.addError("jsonSchema", INVALID_SYNTAX);
        }
    }

    public String toJsonString(JsonNode node) {
        try {
            return node != null ? objectMapper.writeValueAsString(node) : null;
        } catch (JacksonException exception) {
            throw new ValidationException(
                    new ValidationResult("settings", JSON_SERIALIZATION_INVALID)
            );
        }
    }

    public JsonNode fromString(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new ValidationException(
                    new ValidationResult("settings", PERSISTED_JSON_INVALID)
            );
        }
    }

    private Schema parseSchema(String schemaDefinition, ValidationResult result) {
        Schema cached = schemaCache.get(schemaDefinition);
        if (cached != null) {
            return cached;
        }

        try {
            JsonNode schemaNode = objectMapper.readTree(schemaDefinition);
            Schema schema = schemaRegistry.getSchema(schemaNode);
            schemaCache.put(schemaDefinition, schema);
            return schema;
        } catch (Exception exception) {
            result.addError("schema", INVALID_SYNTAX);
            return null;
        }
    }
}
