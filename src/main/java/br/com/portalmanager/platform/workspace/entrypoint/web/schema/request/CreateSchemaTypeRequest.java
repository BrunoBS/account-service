package br.com.portalmanager.platform.workspace.entrypoint.web.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;

import java.util.Set;

public record CreateSchemaTypeRequest(
        String code,
        String name,
        String description,
        Set<String> allowedScopes
) {
    public CreateSchemaTypeInput toInput() {
        return new CreateSchemaTypeInput(code, name, description, allowedScopes);
    }
}
