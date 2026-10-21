package br.com.portalmanager.platform.workspace.foundation.integration;

public interface ApplicationReferencePort {
    Long resolveActive(String workspaceIdentifier, String applicationIdentifier);

    Long resolveActiveForUpdate(String workspaceIdentifier, String applicationIdentifier);
}
