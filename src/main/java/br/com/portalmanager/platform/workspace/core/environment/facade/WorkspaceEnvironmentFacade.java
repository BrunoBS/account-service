package br.com.portalmanager.platform.workspace.core.environment.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTreeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.WorkspaceEnvironmentCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceEnvironmentFacade {

    private final WorkspaceEnvironmentCommandService command;
    private final EnvironmentQueryService query;

    public WorkspaceEnvironmentFacade(WorkspaceEnvironmentCommandService command, EnvironmentQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public EnvironmentOutput create(AuthorizationContext context, String workspaceIdentifier, CreateEnvironmentInput input) {
        return command.create(workspaceIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<EnvironmentOutput> list(AuthorizationContext context, String workspaceIdentifier, String lifecycle) {
        return query.listWorkspace(workspaceIdentifier, lifecycle);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public EnvironmentOutput find(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        return query.findWorkspace(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<EnvironmentOutput> findRoots(AuthorizationContext context, String workspaceIdentifier) {
        return query.findRoots(workspaceIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<EnvironmentOutput> findChildren(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        return query.findChildren(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<EnvironmentTreeOutput> findTree(AuthorizationContext context, String workspaceIdentifier) {
        return query.findTree(workspaceIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public EnvironmentOutput update(AuthorizationContext context, String workspaceIdentifier, String identifier, UpdateEnvironmentInput input) {
        return command.update(workspaceIdentifier, identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        command.inactivate(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.RESTORE)
    public EnvironmentOutput restore(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        return command.restore(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        command.delete(workspaceIdentifier, identifier);
    }
}
