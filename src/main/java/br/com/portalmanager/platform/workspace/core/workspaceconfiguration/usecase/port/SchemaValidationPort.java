package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

import tools.jackson.databind.JsonNode;

public interface SchemaValidationPort {
    void validate(
            String type, String code, String field, JsonNode payload, ValidationResult result);
}
