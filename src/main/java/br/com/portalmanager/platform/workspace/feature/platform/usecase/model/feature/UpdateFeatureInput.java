package br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature;

public record UpdateFeatureInput(Long version, String code, String name, String description, String microserviceIdentifier, String settings, boolean shareable) {}
