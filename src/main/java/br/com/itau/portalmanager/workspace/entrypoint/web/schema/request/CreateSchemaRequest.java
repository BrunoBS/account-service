package br.com.itau.portalmanager.workspace.entrypoint.web.schema.request;

import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaInput;

public record CreateSchemaRequest(
        String schemaTypeCode,
        String code,
        String name,
        String description
) {
    public CreateSchemaInput toPlatformInput() {
        return new CreateSchemaInput(schemaTypeCode, code, name, description, null);
    }

    public CreateSchemaInput toWorkspaceInput(String workspaceIdentifier) {
        return new CreateSchemaInput(schemaTypeCode, code, name, description, workspaceIdentifier);
    }
}
