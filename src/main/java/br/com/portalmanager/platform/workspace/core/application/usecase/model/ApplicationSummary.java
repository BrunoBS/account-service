package br.com.portalmanager.platform.workspace.core.application.usecase.model;

public record ApplicationSummary(
    String identifier,
    String name,
    String alias,
    String acronym,
    String applicationScope,
    String authorizerGroup
) {}
