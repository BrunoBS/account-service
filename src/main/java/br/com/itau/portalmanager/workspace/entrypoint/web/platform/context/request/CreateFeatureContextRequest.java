package br.com.itau.portalmanager.workspace.entrypoint.web.platform.context.request;

import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateFeatureContextInput;

public record CreateFeatureContextRequest(
        String code,
        String name,
        String description
) {
    public CreateFeatureContextInput toInput() {
        return new CreateFeatureContextInput(code, name, description);
    }
}
