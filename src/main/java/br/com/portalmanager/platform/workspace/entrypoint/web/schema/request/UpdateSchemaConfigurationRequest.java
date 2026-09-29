package br.com.portalmanager.platform.workspace.entrypoint.web.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaConfigurationInput;

public record UpdateSchemaConfigurationRequest(Long version, String schemaIdentifier) {
    public UpdateSchemaConfigurationInput toInput() {
        return new UpdateSchemaConfigurationInput(version, schemaIdentifier);
    }
}
