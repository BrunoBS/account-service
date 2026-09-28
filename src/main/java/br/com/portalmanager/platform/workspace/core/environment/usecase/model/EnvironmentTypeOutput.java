package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentType;

public record EnvironmentTypeOutput(Long version, String identifier, String code, String name, String description,
                                    boolean rootAllowed, boolean workspaceRequired, int displayOrder, String lifecycle) {
    public static EnvironmentTypeOutput from(EnvironmentType type) {
        return new EnvironmentTypeOutput(type.getVersion(), type.getIdentifier(), type.getCode(), type.getName(),
                type.getDescription(), type.isRootAllowed(), type.isWorkspaceRequired(), type.getDisplayOrder(),
                type.getLifecycle().value());
    }
}
