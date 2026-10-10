package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateMicroserviceInput;
import tools.jackson.databind.JsonNode;

public record CreateMicroserviceRequest(String code, String name, String description, JsonNode settings) {
    public CreateMicroserviceInput toInput() {
        return new CreateMicroserviceInput(code, name, description, settings == null ? null : settings.toString());
    }
}
