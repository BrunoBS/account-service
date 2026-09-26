package br.com.portalmanager.platform.workspace.entrypoint.web.platform.service.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateServiceInput;

public record UpdateServiceRequest(
        String name,
        String description
) {
    public UpdateServiceInput toInput() {
        return new UpdateServiceInput(name, description);
    }
}
