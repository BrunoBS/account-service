package br.com.portalmanager.platform.workspace.feature.servicecontext.facade.microservice;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.UpdateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MicroserviceFacade {

    private final MicroserviceCommandService commandService;
    private final MicroserviceQueryService queryService;

    public MicroserviceFacade(
            MicroserviceCommandService commandService, MicroserviceQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public MicroserviceOutput create(
            AuthorizationContext authorizationContext, CreateMicroserviceInput input) {
        return commandService.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public MicroserviceOutput findByIdentifier(
            AuthorizationContext authorizationContext, String microserviceIdentifier) {
        return queryService.findByIdentifier(microserviceIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public Page<MicroserviceOutput> findAll(
            AuthorizationContext authorizationContext, String lifecycle, Pageable pageable) {
        return queryService.findAll(lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public MicroserviceOutput update(
            AuthorizationContext authorizationContext,
            String microserviceIdentifier,
            UpdateMicroserviceInput input) {
        return commandService.update(microserviceIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public MicroserviceOutput activate(
            AuthorizationContext authorizationContext, String microserviceIdentifier) {
        return commandService.activate(microserviceIdentifier);
    }

    @AuthorizationRequired(
            level = AuthorizationLevel.OWNER,
            action = AuthorizationAction.DEACTIVATE)
    public MicroserviceOutput inactivate(
            AuthorizationContext authorizationContext, String microserviceIdentifier) {
        return commandService.inactivate(microserviceIdentifier);
    }
}
