package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.integration.JsonSchemaValidationPort;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

/** Shared resolve-and-validate entry point for configurable JSON settings. */
@Component
public class SchemaSettingsValidationAdapter {

    private final SchemaResolutionPort schemaResolutionPort;
    private final JsonSchemaValidationPort jsonSchemaValidationPort;

    public SchemaSettingsValidationAdapter(
            SchemaResolutionPort schemaResolutionPort,
            JsonSchemaValidationPort jsonSchemaValidationPort) {
        this.schemaResolutionPort = schemaResolutionPort;
        this.jsonSchemaValidationPort = jsonSchemaValidationPort;
    }

    public void validateRequired(
            String type,
            String code,
            String attributeName,
            JsonNode value,
            ValidationResult result) {
        jsonSchemaValidationPort.validateJson(
                schemaResolutionPort.resolveRequired(type, code), value, attributeName, result);
    }

    public void validate(
            String type, String code, String attributeName, String value, ValidationResult result) {
        validate(
                type,
                code,
                attributeName,
                jsonSchemaValidationPort.fromString(value, attributeName),
                result);
    }

    public void validate(
            String type,
            String code,
            String attributeName,
            JsonNode value,
            ValidationResult result) {
        jsonSchemaValidationPort.validateJson(
                schemaResolutionPort.resolve(type, code), value, attributeName, result);
    }
}
