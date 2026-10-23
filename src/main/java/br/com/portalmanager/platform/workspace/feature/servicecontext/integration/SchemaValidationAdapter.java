package br.com.portalmanager.platform.workspace.feature.servicecontext.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port.SchemaValidationPort;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;

import org.springframework.stereotype.Component;

@Component("featureservicecontextSchemaValidationAdapter")
public class SchemaValidationAdapter implements SchemaValidationPort {
    private final SchemaSettingsValidationAdapter schemas;

    public SchemaValidationAdapter(SchemaSettingsValidationAdapter schemas) {
        this.schemas = schemas;
    }

    @Override
    public void validate(
            String type, String code, String field, String payload, ValidationResult result) {
        schemas.validate(type, code, field, payload, result);
    }
}
