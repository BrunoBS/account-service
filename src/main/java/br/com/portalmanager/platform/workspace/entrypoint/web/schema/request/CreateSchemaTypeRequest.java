package br.com.portalmanager.platform.workspace.entrypoint.web.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;

public record CreateSchemaTypeRequest(
        String code,
        String name,
        String description,
        String scope
) {
    public CreateSchemaTypeInput toInput() {
        return new CreateSchemaTypeInput(code, name, description, scope);
    }
}
