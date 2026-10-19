package br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype;

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
) {}
