package br.com.itau.portalmanager.workspace.entrypoint.web.platform.context.request;

import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;

public record UpdateFeatureContextRequest(
        String name,
        String description
) {
    public UpdateFeatureContextInput toInput() {
        return new UpdateFeatureContextInput(name, description);
    }
}
