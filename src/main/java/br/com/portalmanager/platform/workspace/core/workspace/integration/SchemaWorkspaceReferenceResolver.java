package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.workspace.WorkspaceReferenceResolver;
import org.springframework.stereotype.Component;

@Component
public class SchemaWorkspaceReferenceResolver implements WorkspaceReferenceResolver {

    private final WorkspaceQueryService workspaceQueryService;

    public SchemaWorkspaceReferenceResolver(WorkspaceQueryService workspaceQueryService) {
        this.workspaceQueryService = workspaceQueryService;
    }

    @Override
    public Long resolveInternalId(String workspaceIdentifier) {
        return workspaceQueryService.findInternalIdByIdentifier(workspaceIdentifier);
    }

    @Override
    public Long resolveActiveInternalId(String workspaceIdentifier) {
        return workspaceQueryService.findActiveInternalIdByIdentifier(workspaceIdentifier);
    }

    @Override
    public String resolveIdentifier(Long workspaceId) {
        return workspaceQueryService.findIdentifierByInternalId(workspaceId);
    }
}
