package br.com.itau.portalmanager.workspace.entrypoint.web.platform.feature.request;

import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.UpdateFeatureInput;

public record UpdateFeatureRequest(
        String name,
        String description,
        String serviceIdentifier,
        String settings
) {
    public UpdateFeatureInput toInput() {
        return new UpdateFeatureInput(name, description, serviceIdentifier, settings);
    }
}
