package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.validation.JsonSchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaMessageKeys;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Shared resolve-and-validate entry point for configurable JSON settings. */
@Component
public class SchemaSettingsValidationAdapter {

    private final SchemaResolutionPort schemaResolutionPort;
    private final JsonSchemaValidator validator;
    private final ObjectMapper objectMapper;

    public SchemaSettingsValidationAdapter(
            SchemaResolutionPort schemaResolutionPort,
            JsonSchemaValidator validator,
            ObjectMapper objectMapper) {
        this.schemaResolutionPort = schemaResolutionPort;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public void validateRequired(
            String type,
            String code,
            String attributeName,
            JsonNode value,
            ValidationResult result) {
        validator.validate(
                schemaResolutionPort.resolveRequired(type, code), value, attributeName, result);
    }

    public void validate(
            String type, String code, String attributeName, String value, ValidationResult result) {
        validate(type, code, attributeName, fromString(value, attributeName), result);
    }

    public void validate(
            String type,
            String code,
            String attributeName,
            JsonNode value,
            ValidationResult result) {
        validator.validate(schemaResolutionPort.resolve(type, code), value, attributeName, result);
    }

    private JsonNode fromString(String value, String field) {
        if (value == null || value.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new ValidationException(
                    new ValidationResult(field, SchemaMessageKeys.PERSISTED_JSON_INVALID));
        }
    }
}
