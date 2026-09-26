package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;

public record UpdateFeatureContextRequest(
        String name,
        String description
) {
    public UpdateFeatureContextInput toInput() {
        return new UpdateFeatureContextInput(name, description);
    }
}
