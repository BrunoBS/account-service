package br.com.portalmanager.platform.workspace.feature.servicecontext.facade.feature;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FeatureFacade {

    private final FeatureCommandService commandService;
    private final FeatureQueryService queryService;

    public FeatureFacade(FeatureCommandService commandService, FeatureQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public FeatureOutput create(
            AuthorizationContext authorizationContext, CreateFeatureInput input) {
        return commandService.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public FeatureOutput findByIdentifier(
            AuthorizationContext authorizationContext, String featureIdentifier) {
        return queryService.findByIdentifier(featureIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public Page<FeatureOutput> findAll(
            AuthorizationContext authorizationContext,
            String featureGroupIdentifier,
            String lifecycle,
            Pageable pageable) {
        return queryService.findAll(featureGroupIdentifier, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureOutput update(
            AuthorizationContext authorizationContext,
            String featureIdentifier,
            UpdateFeatureInput input) {
        return commandService.update(featureIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public FeatureOutput activate(
            AuthorizationContext authorizationContext, String featureIdentifier) {
        return commandService.activate(featureIdentifier);
    }

    @AuthorizationRequired(
            level = AuthorizationLevel.OWNER,
            action = AuthorizationAction.DEACTIVATE)
    public FeatureOutput inactivate(
            AuthorizationContext authorizationContext, String featureIdentifier) {
        return commandService.inactivate(featureIdentifier);
    }
}
