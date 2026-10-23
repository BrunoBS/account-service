package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port;

public interface ApplicationReferencePort {
    Long resolveActive(String workspaceIdentifier, String applicationIdentifier);

    Long resolveActiveForUpdate(String workspaceIdentifier, String applicationIdentifier);
}
