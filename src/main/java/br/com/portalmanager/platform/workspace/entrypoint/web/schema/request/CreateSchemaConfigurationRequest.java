package br.com.portalmanager.platform.workspace.entrypoint.web.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;

public record CreateSchemaConfigurationRequest(String resourceType, String resourceCode, String schemaIdentifier) {
    public CreateSchemaConfigurationInput toInput() {
        return new CreateSchemaConfigurationInput(resourceType, resourceCode, schemaIdentifier);
    }
}
