package br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype;

public record EnvironmentTypeInput(
    Long version,
    String code,
    String name,
    String description,
    Boolean rootAllowed,
    Boolean allowsChildren,
    Boolean workspaceRequired,
    Integer displayOrder
) {}
