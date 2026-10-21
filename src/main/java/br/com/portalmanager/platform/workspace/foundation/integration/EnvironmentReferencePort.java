package br.com.portalmanager.platform.workspace.foundation.integration;

public interface EnvironmentReferencePort {
    Long resolveActiveForUpdate(Long workspaceId, String identifier);

    String resolveIdentifier(Long id);
}
