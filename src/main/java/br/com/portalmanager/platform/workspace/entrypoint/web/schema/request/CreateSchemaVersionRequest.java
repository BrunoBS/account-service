package br.com.portalmanager.platform.workspace.entrypoint.web.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaVersionInput;
import tools.jackson.databind.JsonNode;

public record CreateSchemaVersionRequest(
        String versionName,
        JsonNode definition
) {
    public CreateSchemaVersionInput toInput() {
        return new CreateSchemaVersionInput(versionName, definition);
    }
}
