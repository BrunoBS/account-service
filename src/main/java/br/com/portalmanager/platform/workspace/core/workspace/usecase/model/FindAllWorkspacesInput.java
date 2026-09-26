package br.com.portalmanager.platform.workspace.core.workspace.usecase.model;

public record FindAllWorkspacesInput(
        Boolean active,
        String typeName,
        String tagName
) {
}
