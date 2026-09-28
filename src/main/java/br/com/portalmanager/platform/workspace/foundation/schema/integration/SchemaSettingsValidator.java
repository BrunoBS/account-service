package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaResourceType;
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

    public void validate(SchemaResourceType type, String code, String settings, ValidationResult result) {
        validate(type, code, json.fromString(settings, "settings"), result);
    }

    public void validate(SchemaResourceType type, String code, JsonNode settings, ValidationResult result) {
        json.validateJson(resolver.resolve(type, code), settings, "settings", result);
    }
}
