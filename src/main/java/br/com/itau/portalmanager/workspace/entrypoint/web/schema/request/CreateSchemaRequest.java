package br.com.itau.portalmanager.workspace.entrypoint.web.schema.request;

import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import tools.jackson.databind.JsonNode;

public record CreateSchemaRequest(
        String schemaTypeCode,
        String code,
        String name,
        String description,
        String versionName,
        JsonNode definition
) {
    public CreateSchemaInput toPlatformInput() {
        return new CreateSchemaInput(
                schemaTypeCode,
                code,
                name,
                description,
                null,
                versionName,
                definition
        );
    }

    public CreateSchemaInput toWorkspaceInput(String workspaceIdentifier) {
        return new CreateSchemaInput(
                schemaTypeCode,
                code,
                name,
                description,
                workspaceIdentifier,
                versionName,
                definition
        );
    }
}
