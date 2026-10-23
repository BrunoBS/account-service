package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port;

public interface EnvironmentReferencePort {
    Long resolveActiveForUpdate(Long workspaceId, String identifier);

    String resolveIdentifier(Long id);
}
