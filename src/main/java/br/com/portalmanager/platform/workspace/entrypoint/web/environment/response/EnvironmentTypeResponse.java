package br.com.portalmanager.platform.workspace.entrypoint.web.environment.response;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;

public record EnvironmentTypeResponse(
    Long version,
    String identifier,
    String code,
    String name,
    String description,
    boolean rootAllowed,
    boolean workspaceRequired,
    int displayOrder,
    String lifecycle
) {
    public static EnvironmentTypeResponse from(EnvironmentTypeOutput output) {
        return new EnvironmentTypeResponse(
            output.version(),
            output.identifier(),
            output.code(),
            output.name(),
            output.description(),
            output.rootAllowed(),
            output.workspaceRequired(),
            output.displayOrder(),
            output.lifecycle()
        );
    }
}
