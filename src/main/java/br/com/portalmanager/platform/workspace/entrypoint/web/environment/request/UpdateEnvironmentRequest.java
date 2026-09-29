package br.com.portalmanager.platform.workspace.entrypoint.web.environment.request;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;

public record UpdateEnvironmentRequest(Long version, String name, String description, String authorizationType,
                                       Integer sortOrder, String authorizerGroup, String settings, String environmentTypeCode) {
    public UpdateEnvironmentInput toInput() {
        return new UpdateEnvironmentInput(version, name, description, authorizationType, sortOrder, authorizerGroup,
                settings, environmentTypeCode);
    }
}
