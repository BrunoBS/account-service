package br.com.itau.portalmanager.workspace.feature.platform.usecase.model;

public record CreateFeatureInput(
        String code,
        String name,
        String description,
        String serviceIdentifier,
        String settings
) {
}
