package br.com.portalmanager.platform.workspace.entrypoint.web.application.request;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record UpdateApplicationRequest(Long version, String name, String alias, String acronym, String applicationScope,
                                       String authorizerGroup, JsonNode settings, List<String> tags) {
    public UpdateApplicationInput toInput() {
        return new UpdateApplicationInput(version, name, alias, acronym, applicationScope, authorizerGroup, settings, tags);
    }
}
