package br.com.portalmanager.platform.workspace.entrypoint.web.environment.response;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;

public record EnvironmentTypeResponse(
    Long version,
    String code,
    String name,
    String description,
    boolean rootAllowed,
    boolean allowsChildren,
    boolean workspaceRequired,
    int displayOrder,
    String lifecycle
) {
    public static EnvironmentTypeResponse from(EnvironmentTypeOutput output) {
        return new EnvironmentTypeResponse(
            output.version(),
            output.code(),
            output.name(),
            output.description(),
            output.rootAllowed(),
            output.allowsChildren(),
            output.workspaceRequired(),
            output.displayOrder(),
            output.lifecycle()
        );
    }
}
