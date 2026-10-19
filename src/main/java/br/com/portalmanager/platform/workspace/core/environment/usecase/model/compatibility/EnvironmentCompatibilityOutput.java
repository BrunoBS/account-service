package br.com.portalmanager.platform.workspace.core.environment.usecase.model.compatibility;

public record EnvironmentCompatibilityOutput(
    String identifier,
    String parentTypeCode,
    String childTypeCode,
    String lifecycle
) {}
