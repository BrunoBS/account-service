package br.com.portalmanager.platform.workspace.core.workspace.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.FindAllWorkspacesInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceCommandService;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceNormalizer;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceFacade {

    private final WorkspaceCommandService commandService;
    private final WorkspaceQueryService queryService;
    private final WorkspaceNormalizer normalizer;

    public WorkspaceFacade(
        WorkspaceCommandService commandService,
        WorkspaceQueryService queryService,
        WorkspaceNormalizer normalizer
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.normalizer = normalizer;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.CREATE)
    public WorkspaceOutput create(AuthorizationContext context, CreateWorkspaceInput input) {
        return commandService.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public WorkspaceOutput findByIdentifier(AuthorizationContext context, String identifier) {
        return queryService.findByIdentifier(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public Page<WorkspaceOutput> findAll(
        AuthorizationContext context,
        String lifecycle,
        String typeName,
        String tagName,
        Pageable pageable
    ) {
        FindAllWorkspacesInput input = new FindAllWorkspacesInput(
            lifecycle,
            normalizer.normalizeTypeFilter(typeName),
            normalizer.normalizeTagFilter(tagName)
        );
        return queryService.findAll(input, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public WorkspaceOutput update(AuthorizationContext context, String identifier, UpdateWorkspaceInput input) {
        return commandService.update(identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String identifier) {
        commandService.inactivate(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.RESTORE)
    public WorkspaceOutput restore(AuthorizationContext context, String identifier) {
        return commandService.restore(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String identifier) {
        commandService.delete(identifier);
    }
}
