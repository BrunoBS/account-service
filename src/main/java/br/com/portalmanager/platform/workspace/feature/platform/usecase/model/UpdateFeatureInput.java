package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

public record UpdateFeatureInput(
    String name,
    String description,
    String microserviceIdentifier,
    String settings,
    boolean shareable
) {}
