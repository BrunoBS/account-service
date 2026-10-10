package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

public record CreateEnvironmentInput(
    String name,
    String description,
    String authorizationType,
    Integer sortOrder,
    String authorizerGroup,
    String settings,
    String environmentTypeCode,
    String parentIdentifier
) {}
