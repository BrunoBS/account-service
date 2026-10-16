package br.com.portalmanager.platform.workspace.feature.servicecontext.facade.featuregroup;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.CreateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.FeatureGroupOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.UpdateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FeatureGroupFacade {

    private final FeatureGroupCommandService commandService;
    private final FeatureGroupQueryService queryService;

    public FeatureGroupFacade(FeatureGroupCommandService commandService, FeatureGroupQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public FeatureGroupOutput create(AuthorizationContext authorizationContext, CreateFeatureGroupInput input) {
        return commandService.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public FeatureGroupOutput findByIdentifier(
            AuthorizationContext authorizationContext, String featureGroupIdentifier) {
        return queryService.findByIdentifier(featureGroupIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public Page<FeatureGroupOutput> findAll(
            AuthorizationContext authorizationContext,
            String featureIdentifier,
            String lifecycle,
            Pageable pageable) {
        return queryService.findAll(featureIdentifier, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureGroupOutput update(
            AuthorizationContext authorizationContext,
            String featureGroupIdentifier,
            UpdateFeatureGroupInput input) {
        return commandService.update(featureGroupIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public FeatureGroupOutput activate(AuthorizationContext authorizationContext, String featureGroupIdentifier) {
        return commandService.activate(featureGroupIdentifier);
    }

    @AuthorizationRequired(
            level = AuthorizationLevel.OWNER,
            action = AuthorizationAction.DEACTIVATE)
    public FeatureGroupOutput inactivate(
            AuthorizationContext authorizationContext, String featureGroupIdentifier) {
        return commandService.inactivate(featureGroupIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public FeatureGroupOutput purge(AuthorizationContext authorizationContext, String featureGroupIdentifier) {
        return commandService.purge(featureGroupIdentifier);
    }
}
