package br.com.portalmanager.platform.workspace.core.workspace.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.FindAllWorkspacesInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceCommandService;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request.CreateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request.UpdateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.response.WorkspaceResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class WorkspaceFacade {
    private final WorkspaceCommandService command;
    private final WorkspaceQueryService query;

    public WorkspaceFacade(WorkspaceCommandService command, WorkspaceQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.CREATE)
    public WorkspaceResponse create(CreateWorkspaceRequest request) {
        return WorkspaceResponse.from(command.create(request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public WorkspaceResponse findByIdentifier(String identifier) {
        return WorkspaceResponse.from(query.findByIdentifier(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<WorkspaceResponse> findAll(Boolean active, String typeName, String tagName) {
        return query.findAll(new FindAllWorkspacesInput(active, typeName, tagName))
                .stream().map(WorkspaceResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public WorkspaceResponse update(String identifier, UpdateWorkspaceRequest request) {
        return WorkspaceResponse.from(command.update(identifier, request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(String identifier) {
        command.inactivate(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.RESTORE)
    public WorkspaceResponse restore(String identifier) {
        return WorkspaceResponse.from(command.restore(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(String identifier) {
        command.delete(identifier);
    }
}
