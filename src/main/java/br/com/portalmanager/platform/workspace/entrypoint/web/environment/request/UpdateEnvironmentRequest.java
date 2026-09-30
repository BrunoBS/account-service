package br.com.portalmanager.platform.workspace.entrypoint.web.environment.request;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import tools.jackson.databind.JsonNode;

public record UpdateEnvironmentRequest(Long version, String name, String description, String authorizationType,
                                       Integer sortOrder, String authorizerGroup, JsonNode settings, String environmentTypeCode) {
    public UpdateEnvironmentInput toInput() {
        return new UpdateEnvironmentInput(version, name, description, authorizationType, sortOrder, authorizerGroup,
                settings == null ? null : settings.toString(), environmentTypeCode);
    }
}
