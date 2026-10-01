package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkspaceReferenceResolver implements WorkspaceReferenceResolver {

    private final WorkspaceFinder workspaceFinder;
    private final WorkspaceValidator validator;

    public DefaultWorkspaceReferenceResolver(WorkspaceFinder workspaceFinder, WorkspaceValidator validator) {
        this.workspaceFinder = workspaceFinder;
        this.validator = validator;
    }

    @Override
    public Long resolveInternalId(String workspaceIdentifier) {
        Workspace workspace = workspaceFinder.findByIdentifier(workspaceIdentifier);
        validator.requireActive(workspace);
        return workspace.getId();
    }

    @Override
    public String resolveIdentifier(Long workspaceId) {
        Workspace workspace = workspaceFinder.findById(workspaceId);
        validator.requireActive(workspace);
        return workspace.getIdentifier();
    }

    @Override
    public String resolveWorkspaceType(String workspaceIdentifier) {
        Workspace workspace = workspaceFinder.findByIdentifier(workspaceIdentifier);
        validator.requireActive(workspace);
        return workspace.getWorkspaceType().value();
    }
}
