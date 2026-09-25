package br.com.itau.portalmanager.workspace.feature.platform.usecase.model;

public record CreateFeatureContextInput(
        String code,
        String name,
        String description
) {
}
