package br.com.portalmanager.platform.workspace.entrypoint.schema.request;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema.UpdateSchemaInput;

public record UpdateSchemaRequest(Long version, String name, String description) {
    public UpdateSchemaInput toInput() {
        return new UpdateSchemaInput(version, name, description);
    }
}
