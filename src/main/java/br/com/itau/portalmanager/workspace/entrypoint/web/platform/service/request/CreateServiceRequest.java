package br.com.itau.portalmanager.workspace.entrypoint.web.platform.service.request;

import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateServiceInput;

public record CreateServiceRequest(
        String code,
        String name,
        String description
) {
    public CreateServiceInput toInput() {
        return new CreateServiceInput(code, name, description);
    }
}
