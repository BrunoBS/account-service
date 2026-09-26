package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureInput;

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
