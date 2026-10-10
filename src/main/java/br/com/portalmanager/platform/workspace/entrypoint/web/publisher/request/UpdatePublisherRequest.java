package br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request;

import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.UpdatePublisherInput;
import tools.jackson.databind.JsonNode;

public record UpdatePublisherRequest(
    Long version,
    String name,
    String description,
    String scope,
    Boolean deprecated,
    JsonNode settings
) {
    public UpdatePublisherInput toInput() {
        return new UpdatePublisherInput(
            version,
            name,
            description,
            scope,
            deprecated,
            settings == null ? null : settings.toString()
        );
    }
}
