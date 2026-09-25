package br.com.itau.portalmanager.workspace.entrypoint.web.platform.service.request;

import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.UpdateServiceInput;

public record UpdateServiceRequest(
        String name,
        String description
) {
    public UpdateServiceInput toInput() {
        return new UpdateServiceInput(name, description);
    }
}
