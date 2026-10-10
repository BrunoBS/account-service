package br.com.portalmanager.platform.workspace.entrypoint.web.environment.request;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;

public record UpdateEnvironmentTypeRequest(
    Long version,
    String code,
    String name,
    String description,
    Boolean rootAllowed,
    Boolean workspaceRequired,
    Integer displayOrder
) {
    public EnvironmentTypeInput toInput() {
        return new EnvironmentTypeInput(version, code, name, description, rootAllowed, workspaceRequired, displayOrder);
    }
}
