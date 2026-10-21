package br.com.portalmanager.platform.workspace.core.configuration.application.usecase.operations;

import br.com.portalmanager.platform.workspace.foundation.integration.ApplicationReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferencePort;

import org.springframework.stereotype.Component;

@Component
public class ApplicationEnvironmentConfigurationScope {
    private final WorkspaceReferencePort workspaces;
    private final ApplicationReferencePort applications;

    public ApplicationEnvironmentConfigurationScope(
            WorkspaceReferencePort workspaces, ApplicationReferencePort applications) {
        this.workspaces = workspaces;
        this.applications = applications;
    }

    public Scope resolve(
            String workspaceIdentifier, String applicationIdentifier, boolean mutation) {
        Long workspaceId =
                mutation
                        ? workspaces.resolveActiveForUpdate(workspaceIdentifier).id()
                        : workspaces.resolveActive(workspaceIdentifier).id();
        Long ownerId =
                (mutation
                        ? applications.resolveActiveForUpdate(
                                workspaceIdentifier, applicationIdentifier)
                        : applications.resolveActive(workspaceIdentifier, applicationIdentifier));
        return new Scope(workspaceId, ownerId);
    }

    public record Scope(Long workspaceId, Long ownerId) {}
}
