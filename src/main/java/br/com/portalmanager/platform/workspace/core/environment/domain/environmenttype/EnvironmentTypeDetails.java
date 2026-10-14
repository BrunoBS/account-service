package br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype;

public record EnvironmentTypeDetails(
    String name,
    String description,
    boolean rootAllowed,
    boolean allowsChildren,
    boolean workspaceRequired,
    int displayOrder
) {}
