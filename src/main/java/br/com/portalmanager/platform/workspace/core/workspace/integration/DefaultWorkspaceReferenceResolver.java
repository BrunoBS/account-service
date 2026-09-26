package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkspaceReferenceResolver implements WorkspaceReferenceResolver {

    private final WorkspaceQueryService workspaceQueryService;

    public DefaultWorkspaceReferenceResolver(WorkspaceQueryService workspaceQueryService) {
        this.workspaceQueryService = workspaceQueryService;
    }

    @Override
    public Long resolveInternalId(String workspaceIdentifier) {
        return workspaceQueryService.findInternalIdByIdentifier(workspaceIdentifier);
    }

    @Override
    public String resolveIdentifier(Long workspaceId) {
        return workspaceQueryService.findIdentifierByInternalId(workspaceId);
    }
}
