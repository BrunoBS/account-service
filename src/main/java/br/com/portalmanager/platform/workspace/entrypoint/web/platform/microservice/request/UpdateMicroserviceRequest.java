package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateMicroserviceInput;
import tools.jackson.databind.JsonNode;

public record UpdateMicroserviceRequest(
        String name,
        String description,
        JsonNode settings
) {
    public UpdateMicroserviceInput toInput() {
        return new UpdateMicroserviceInput(name, description, settings == null ? null : settings.toString());
    }
}
