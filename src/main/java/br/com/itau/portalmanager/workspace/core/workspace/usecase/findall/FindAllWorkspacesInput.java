package br.com.itau.portalmanager.workspace.core.workspace.usecase.findall;

public record FindAllWorkspacesInput(
        Boolean active,
        String typeName,
        String tagName
) {
}
