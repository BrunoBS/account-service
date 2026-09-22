package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support;

import br.com.portalmanager.platform.catalog.validation.CatalogValidationResult;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import br.com.portalmanager.platform.messaging.validation.ValidationResult;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class CatalogSettingsSchemaValidator {

    public static final String DEFAULT_JSON_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "title": "Default Dynamic Schema",
              "type": "object",
              "additionalProperties": {
                "type": ["string", "number", "boolean", "null"]
              }
            }
            """;

    private static final String VALUE_REQUIRED = "workspace-service.catalog.settings.value.required";
    private static final String PERSISTED_JSON_INVALID = "workspace-service.catalog.settings.persisted.json.invalid";
    private static final String INVALID = "workspace-service.catalog.settings.invalid";

    private final ObjectMapper objectMapper;
    private final Schema schema;

    public CatalogSettingsSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        try {
            JsonNode schemaNode = objectMapper.readTree(DEFAULT_JSON_SCHEMA);
            this.schema = SchemaRegistry
                    .withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                    .getSchema(schemaNode);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize catalog settings schema", exception);
        }
    }

    public void validate(JsonNode settings, String attributeName, CatalogValidationResult result) {
        String field = attributeName == null ? "settings" : attributeName;
        if (settings == null || settings.isNull()) {
            result.addError(field, VALUE_REQUIRED, java.util.Map.of("0", field));
            return;
        }

        if (!schema.validate(settings).isEmpty()) {
            result.addError(field, INVALID);
        }
    }

    public JsonNode fromString(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new ValidationException(new ValidationResult("settings", PERSISTED_JSON_INVALID));
        }
    }
}
