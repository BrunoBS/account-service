package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

public record EnvironmentTypeInput(
    Long version,
    String code,
    String name,
    String description,
    Boolean rootAllowed,
    Boolean workspaceRequired,
    Integer displayOrder
) {}
