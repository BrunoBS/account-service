package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Shared resolve-and-validate entry point for configurable JSON settings. */
@Component
public class SchemaSettingsValidator {

    private final SchemaResolutionPort resolver;
    private final JsonSchemaValidator json;

    public SchemaSettingsValidator(SchemaResolutionPort resolver, JsonSchemaValidator json) {
        this.resolver = resolver;
        this.json = json;
    }

    public void validate(String type, String code, String attributeName, String value, ValidationResult result) {
        validate(type, code, attributeName, json.fromString(value, attributeName), result);
    }

    public void validate(String type, String code, String attributeName, JsonNode value, ValidationResult result) {
        json.validateJson(resolver.resolve(type, code), value, attributeName, result);
    }
}
