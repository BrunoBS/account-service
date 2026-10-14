package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;

public record EnvironmentTypeOutput(
    Long version,
    // The audit library resolves resource identity from identifier or id; this aliases the immutable code internally.
    String identifier,
    String code,
    String name,
    String description,
    boolean rootAllowed,
    boolean allowsChildren,
    boolean workspaceRequired,
    int displayOrder,
    String lifecycle
) {
    public static EnvironmentTypeOutput from(EnvironmentType type) {
        return new EnvironmentTypeOutput(
            type.getVersion(),
            type.getCode(),
            type.getCode(),
            type.getName(),
            type.getDescription(),
            type.isRootAllowed(),
            type.allowsChildren(),
            type.isWorkspaceRequired(),
            type.getDisplayOrder(),
            type.getLifecycle().value()
        );
    }
}
