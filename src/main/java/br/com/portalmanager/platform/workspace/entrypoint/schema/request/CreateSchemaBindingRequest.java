package br.com.portalmanager.platform.workspace.entrypoint.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.CreateSchemaBindingInput;

public record CreateSchemaBindingRequest(String resourceType, String resourceCode, String schemaIdentifier) {
    public CreateSchemaBindingInput toInput() {
        return new CreateSchemaBindingInput(resourceType, resourceCode, schemaIdentifier);
    }
}
