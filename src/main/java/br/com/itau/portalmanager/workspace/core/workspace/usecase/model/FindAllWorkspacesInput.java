package br.com.itau.portalmanager.workspace.core.workspace.usecase.model;

public record FindAllWorkspacesInput(
        Boolean active,
        String typeName,
        String tagName
) {
}
