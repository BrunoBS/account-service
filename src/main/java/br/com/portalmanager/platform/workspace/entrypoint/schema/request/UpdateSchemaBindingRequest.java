package br.com.portalmanager.platform.workspace.entrypoint.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.UpdateSchemaBindingInput;

public record UpdateSchemaBindingRequest(Long version, String schemaIdentifier) {
    public UpdateSchemaBindingInput toInput() {
        return new UpdateSchemaBindingInput(version, schemaIdentifier);
    }
}
