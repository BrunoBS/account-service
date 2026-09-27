package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

public record UpdateEnvironmentInput(Long version, String name, String description, String authorizationType,
                                     Integer sortOrder, String authorizerGroup, String settings) {}
