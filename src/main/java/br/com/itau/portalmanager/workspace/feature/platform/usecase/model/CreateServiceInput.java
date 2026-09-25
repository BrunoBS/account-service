package br.com.itau.portalmanager.workspace.feature.platform.usecase.model;

public record CreateServiceInput(
        String code,
        String name,
        String description
) {
}
