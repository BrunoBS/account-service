package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateMicroserviceInput;

public record UpdateMicroserviceRequest(
        String name,
        String description
) {
    public UpdateMicroserviceInput toInput() {
        return new UpdateMicroserviceInput(name, description);
    }
}
