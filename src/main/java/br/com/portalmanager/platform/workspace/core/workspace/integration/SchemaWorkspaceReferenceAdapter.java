package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.port.WorkspaceReferencePort;

import org.springframework.stereotype.Component;

@Component
public class SchemaWorkspaceReferenceAdapter implements WorkspaceReferencePort {

    private final WorkspaceFinder workspaceFinder;

    public SchemaWorkspaceReferenceAdapter(WorkspaceFinder workspaceFinder) {
        this.workspaceFinder = workspaceFinder;
    }

    @Override
    public Long resolveInternalId(String workspaceIdentifier) {
        WorkspaceReference workspace = workspaceFinder.findActiveReference(workspaceIdentifier);
        return workspace.getId();
    }

    @Override
    public String resolveIdentifier(Long workspaceId) {
        WorkspaceReference workspace = workspaceFinder.findActiveReference(workspaceId);
        return workspace.getIdentifier();
    }
}
