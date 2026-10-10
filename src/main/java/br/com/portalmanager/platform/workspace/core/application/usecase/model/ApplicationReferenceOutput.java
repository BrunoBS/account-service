package br.com.portalmanager.platform.workspace.core.application.usecase.model;

public record ApplicationReferenceOutput(
    Long id,
    Long workspaceId,
    String identifier,
    String workspaceIdentifier,
    String name
) {}
