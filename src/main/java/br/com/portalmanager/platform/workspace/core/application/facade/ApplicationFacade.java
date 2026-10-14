package br.com.portalmanager.platform.workspace.core.application.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationSummary;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationCommandService;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ApplicationFacade {

    private final ApplicationCommandService commandService;
    private final ApplicationQueryService queryService;

    public ApplicationFacade(ApplicationCommandService commandService, ApplicationQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ApplicationOutput create(
        AuthorizationContext context,
        String workspaceIdentifier,
        CreateApplicationInput input
    ) {
        return commandService.create(workspaceIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ApplicationOutput find(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        return queryService.findByIdentifier(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public Page<ApplicationOutput> findAll(
        AuthorizationContext context,
        String workspaceIdentifier,
        String lifecycle,
        String tagName,
        Pageable pageable
    ) {
        return queryService.findAll(workspaceIdentifier, lifecycle, tagName, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<ApplicationSummary> summary(AuthorizationContext context, String workspaceIdentifier) {
        return queryService.summary(workspaceIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.UPDATE)
    public ApplicationOutput update(
        AuthorizationContext context,
        String workspaceIdentifier,
        String identifier,
        UpdateApplicationInput input
    ) {
        return commandService.update(workspaceIdentifier, identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        commandService.inactivate(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.RESTORE)
    public ApplicationOutput restore(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        return commandService.restore(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String workspaceIdentifier, String identifier) {
        commandService.delete(workspaceIdentifier, identifier);
    }
}
