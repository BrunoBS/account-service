package br.com.portalmanager.platform.workspace.feature.servicecontext.facade.featuregroup;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupAssociationCommandService;

import org.springframework.stereotype.Service;

@Service
public class FeatureGroupAssociationFacade {
    private final FeatureGroupAssociationCommandService commandService;

    public FeatureGroupAssociationFacade(FeatureGroupAssociationCommandService commandService) {
        this.commandService = commandService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureOutput associate(
            AuthorizationContext authorizationContext,
            String featureGroupIdentifier,
            String featureIdentifier) {
        return commandService.associate(featureGroupIdentifier, featureIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureOutput remove(
            AuthorizationContext authorizationContext,
            String featureGroupIdentifier,
            String featureIdentifier) {
        return commandService.remove(featureGroupIdentifier, featureIdentifier);
    }
}
