package br.com.portalmanager.platform.workspace.entrypoint.web.environment.request;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;

public record CreateEnvironmentTypeRequest(
    String code,
    String name,
    String description,
    Boolean rootAllowed,
    Boolean workspaceRequired,
    Integer displayOrder
) {
    public EnvironmentTypeInput toInput() {
        return new EnvironmentTypeInput(null, code, name, description, rootAllowed, workspaceRequired, displayOrder);
    }
}
