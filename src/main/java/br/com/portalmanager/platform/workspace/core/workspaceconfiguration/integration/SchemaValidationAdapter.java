package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.SchemaValidationPort;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component("coreworkspaceconfigurationSchemaValidationAdapter")
public class SchemaValidationAdapter implements SchemaValidationPort {
    private final SchemaSettingsValidationAdapter schemas;

    public SchemaValidationAdapter(SchemaSettingsValidationAdapter schemas) {
        this.schemas = schemas;
    }

    @Override
    public void validate(
            String type, String code, String field, JsonNode payload, ValidationResult result) {
        schemas.validate(type, code, field, payload, result);
    }
}
