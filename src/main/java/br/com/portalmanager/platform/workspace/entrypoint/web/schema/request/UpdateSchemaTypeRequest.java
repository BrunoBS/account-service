package br.com.portalmanager.platform.workspace.entrypoint.web.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaTypeInput;

import java.util.Set;

public record UpdateSchemaTypeRequest(
        Long version,
        String name,
        String description,
        Set<String> allowedScopes
) {
    public UpdateSchemaTypeInput toInput() {
        return new UpdateSchemaTypeInput(version, name, description, allowedScopes);
    }
}
