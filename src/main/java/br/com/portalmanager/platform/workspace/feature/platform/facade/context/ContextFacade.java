package br.com.portalmanager.platform.workspace.feature.platform.facade.context;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.ContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.CreateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.UpdateContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ContextFacade {

    private final ContextCommandService commandService;
    private final ContextQueryService queryService;

    public ContextFacade(ContextCommandService commandService, ContextQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public ContextOutput create(AuthorizationContext authorizationContext, CreateContextInput input) {
        return commandService.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public ContextOutput findByIdentifier(AuthorizationContext authorizationContext, String contextIdentifier) {
        return queryService.findByIdentifier(contextIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public Page<ContextOutput> findAll(AuthorizationContext authorizationContext, String featureIdentifier,
        String lifecycle, Pageable pageable) {
        return queryService.findAll(featureIdentifier, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public ContextOutput update(AuthorizationContext authorizationContext, String contextIdentifier, UpdateContextInput input) {
        return commandService.update(contextIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public ContextOutput activate(AuthorizationContext authorizationContext, String contextIdentifier) {
        return commandService.activate(contextIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public ContextOutput inactivate(AuthorizationContext authorizationContext, String contextIdentifier) {
        return commandService.inactivate(contextIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public ContextOutput purge(AuthorizationContext authorizationContext, String contextIdentifier) {
        return commandService.purge(contextIdentifier);
    }
}
