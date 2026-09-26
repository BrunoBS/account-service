package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

public record CreateFeatureInput(
        String code,
        String name,
        String description,
        String microserviceIdentifier,
        String settings
) {
}
