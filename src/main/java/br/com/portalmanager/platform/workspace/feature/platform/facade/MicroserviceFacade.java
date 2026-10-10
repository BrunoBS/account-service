package br.com.portalmanager.platform.workspace.feature.platform.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.*;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MicroserviceFacade {

    private final MicroserviceCommandService commandService;
    private final MicroserviceQueryService queryService;

    public MicroserviceFacade(MicroserviceCommandService c, MicroserviceQueryService q) {
        commandService = c;
        queryService = q;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public MicroserviceOutput create(AuthorizationContext context, CreateMicroserviceInput i) {
        return commandService.create(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public MicroserviceOutput findByIdentifier(AuthorizationContext context, String i) {
        return queryService.findByIdentifier(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public List<MicroserviceOutput> findAll(AuthorizationContext context) {
        return queryService.findAll();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public MicroserviceOutput update(AuthorizationContext context, String i, UpdateMicroserviceInput in) {
        return commandService.update(i, in);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public MicroserviceOutput activate(AuthorizationContext context, String i) {
        return commandService.activate(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public MicroserviceOutput inactivate(AuthorizationContext context, String i) {
        return commandService.inactivate(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String i) {
        commandService.delete(i);
    }
}
