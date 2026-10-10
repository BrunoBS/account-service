package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureInput;
import tools.jackson.databind.JsonNode;

public record CreateFeatureRequest(
    String code,
    String name,
    String description,
    String microserviceIdentifier,
    JsonNode settings,
    boolean shareable
) {
    public CreateFeatureInput toInput() {
        return new CreateFeatureInput(
            code,
            name,
            description,
            microserviceIdentifier,
            settings == null ? null : settings.toString(),
            shareable
        );
    }
}
