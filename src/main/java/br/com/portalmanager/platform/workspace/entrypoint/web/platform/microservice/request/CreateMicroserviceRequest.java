package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateMicroserviceInput;

public record CreateMicroserviceRequest(
        String code,
        String name,
        String description
) {
    public CreateMicroserviceInput toInput() {
        return new CreateMicroserviceInput(code, name, description);
    }
}
