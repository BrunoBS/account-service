package br.com.portalmanager.platform.workspace.core.environment.usecase.port;

public interface WorkspaceReferencePort {

    Long resolveActiveForUpdate(String workspaceIdentifier);

    Long resolveInternalId(String workspaceIdentifier);
}
