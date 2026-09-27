package br.com.portalmanager.platform.workspace.entrypoint.web.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaTypeInput;

public record UpdateSchemaTypeRequest(
        Long version,
        String name,
        String description,
        String scope
) {
    public UpdateSchemaTypeInput toInput() {
        return new UpdateSchemaTypeInput(version, name, description, scope);
    }
}
