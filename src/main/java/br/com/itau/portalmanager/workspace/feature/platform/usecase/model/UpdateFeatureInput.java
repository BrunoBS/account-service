package br.com.itau.portalmanager.workspace.feature.platform.usecase.model;

public record UpdateFeatureInput(
        String name,
        String description,
        String serviceIdentifier,
        String settings
) {
}
