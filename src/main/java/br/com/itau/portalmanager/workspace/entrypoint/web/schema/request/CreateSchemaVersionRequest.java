package br.com.itau.portalmanager.workspace.entrypoint.web.schema.request;

import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaVersionInput;
import tools.jackson.databind.JsonNode;

public record CreateSchemaVersionRequest(
        String versionName,
        JsonNode definition
) {
    public CreateSchemaVersionInput toInput() {
        return new CreateSchemaVersionInput(versionName, definition);
    }
}
