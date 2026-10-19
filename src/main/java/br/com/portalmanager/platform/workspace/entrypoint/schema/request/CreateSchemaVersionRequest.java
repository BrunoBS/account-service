package br.com.portalmanager.platform.workspace.entrypoint.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.version.CreateSchemaVersionInput;
import tools.jackson.databind.JsonNode;

public record CreateSchemaVersionRequest(String versionName, JsonNode definition) {
    public CreateSchemaVersionInput toInput() {
        return new CreateSchemaVersionInput(versionName, definition);
    }
}
