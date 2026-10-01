package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureInput;
import tools.jackson.databind.JsonNode;

public record UpdateFeatureRequest(
        String name,
        String description,
        String microserviceIdentifier,
        JsonNode settings
) {
    public UpdateFeatureInput toInput() {
        return new UpdateFeatureInput(name, description, microserviceIdentifier,
                settings);
    }
}
