package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureInput;

public record CreateFeatureRequest(
        String code,
        String name,
        String description,
        String serviceIdentifier,
        String settings
) {
    public CreateFeatureInput toInput() {
        return new CreateFeatureInput(code, name, description, serviceIdentifier, settings);
    }
}
