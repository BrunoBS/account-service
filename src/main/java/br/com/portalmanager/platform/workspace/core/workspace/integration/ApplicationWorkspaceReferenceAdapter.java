package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.application.usecase.port.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class ApplicationWorkspaceReferenceAdapter implements WorkspaceReferencePort {

    private final WorkspaceFinder workspaceFinder;
    private final WorkspaceQueryService queries;

    public ApplicationWorkspaceReferenceAdapter(
            WorkspaceFinder workspaceFinder, WorkspaceQueryService queries) {
        this.workspaceFinder = workspaceFinder;
        this.queries = queries;
    }

    @Override
    public ResolvedWorkspace resolveActive(String workspaceIdentifier) {
        WorkspaceReference workspace = workspaceFinder.findActiveReference(workspaceIdentifier);
        return new ResolvedWorkspace(
                workspace.getId(), workspace.getIdentifier(), workspace.getWorkspaceType());
    }

    @Override
    public ResolvedWorkspace resolveActiveForUpdate(String workspaceIdentifier) {
        Workspace workspace = workspaceFinder.findActiveForUpdate(workspaceIdentifier);
        return new ResolvedWorkspace(
                workspace.getId(), workspace.getIdentifier(), workspace.getWorkspaceType().value());
    }

    @Override
    public Long resolveInternalId(String workspaceIdentifier) {
        WorkspaceReference workspace = workspaceFinder.findActiveReference(workspaceIdentifier);
        return workspace.getId();
    }

    @Override
    public Map<Long, String> findReferenceIdentifiersForResourceSharing(Collection<Long> ids) {
        return queries.findReferenceIdentifiersForResourceSharing(ids);
    }

    @Override
    public Map<Long, String> findActiveIdentifiersForResourceSharing(Collection<Long> ids) {
        return queries.findActiveIdentifiersForResourceSharing(ids);
    }
}
