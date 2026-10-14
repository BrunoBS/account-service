package br.com.portalmanager.platform.workspace.feature.platform.facade.featurecontext;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.featurecontext.FeatureContextCommandService;
import org.springframework.stereotype.Service;

@Service
public class FeatureContextFacade {
    private final FeatureContextCommandService commandService;

    public FeatureContextFacade(FeatureContextCommandService commandService) {
        this.commandService = commandService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureOutput associate(AuthorizationContext authorizationContext, String contextIdentifier, String featureIdentifier) {
        return commandService.associate(contextIdentifier, featureIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureOutput remove(AuthorizationContext authorizationContext, String contextIdentifier, String featureIdentifier) {
        return commandService.remove(contextIdentifier, featureIdentifier);
    }
}
