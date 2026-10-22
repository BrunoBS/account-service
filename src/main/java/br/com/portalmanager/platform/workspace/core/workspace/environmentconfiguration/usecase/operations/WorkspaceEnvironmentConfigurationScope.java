package br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.operations;

import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferencePort;

import org.springframework.stereotype.Component;

@Component
public class WorkspaceEnvironmentConfigurationScope {
    private final WorkspaceReferencePort workspaces;

    public WorkspaceEnvironmentConfigurationScope(WorkspaceReferencePort workspaces) {
        this.workspaces = workspaces;
    }

    public Scope resolve(String workspaceIdentifier, boolean mutation) {
        Long workspaceId =
                mutation
                        ? workspaces.resolveActiveForUpdate(workspaceIdentifier).id()
                        : workspaces.resolveActive(workspaceIdentifier).id();
        Long ownerId = workspaceId;
        return new Scope(workspaceId, ownerId);
    }

    public record Scope(Long workspaceId, Long ownerId) {}
}
