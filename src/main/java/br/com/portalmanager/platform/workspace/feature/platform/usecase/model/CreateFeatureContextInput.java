package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

public record CreateFeatureContextInput(
        String code,
        String name,
        String description
) {
}
