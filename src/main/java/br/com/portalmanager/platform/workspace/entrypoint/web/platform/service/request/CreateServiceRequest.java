package br.com.portalmanager.platform.workspace.entrypoint.web.platform.service.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateServiceInput;

public record CreateServiceRequest(
        String code,
        String name,
        String description
) {
    public CreateServiceInput toInput() {
        return new CreateServiceInput(code, name, description);
    }
}
