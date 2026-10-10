package br.com.portalmanager.platform.workspace.entrypoint.web.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import tools.jackson.databind.JsonNode;

public record CreateSchemaRequest(
    String code,
    String name,
    String description,
    String versionName,
    JsonNode definition
) {
    public CreateSchemaInput toPlatformInput() {
        return new CreateSchemaInput(code, name, description, null, versionName, definition);
    }

    public CreateSchemaInput toWorkspaceInput(String workspaceIdentifier) {
        return new CreateSchemaInput(code, name, description, workspaceIdentifier, versionName, definition);
    }
}
