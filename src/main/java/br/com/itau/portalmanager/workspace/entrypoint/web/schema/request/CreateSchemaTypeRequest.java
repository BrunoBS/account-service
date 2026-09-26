package br.com.itau.portalmanager.workspace.entrypoint.web.schema.request;

import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;

public record CreateSchemaTypeRequest(
        String code,
        String name,
        String description
) {
    public CreateSchemaTypeInput toInput() {
        return new CreateSchemaTypeInput(code, name, description);
    }
}
