package br.com.portalmanager.platform.workspace.entrypoint.web.environment.request;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;

public record CreateEnvironmentRequest(String name, String description, String authorizationType,
                                       Integer sortOrder, String authorizerGroup, String settings,
                                       String environmentTypeCode, String parentIdentifier) {
    public CreateEnvironmentInput toInput() {
        return new CreateEnvironmentInput(name, description, authorizationType, sortOrder, authorizerGroup,
                settings, environmentTypeCode, parentIdentifier);
    }
}
