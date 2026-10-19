package br.com.portalmanager.platform.workspace.feature.onboarding.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.progress.OnboardingProgressOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.progress.OnboardingCommandService;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.progress.OnboardingQueryService;

import org.springframework.stereotype.Service;

@Service
public class OnboardingFacade {

    private final OnboardingQueryService queryService;
    private final OnboardingCommandService commandService;

    public OnboardingFacade(
            OnboardingQueryService queryService, OnboardingCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public OnboardingProgressOutput findProgress(
            AuthorizationContext context, String workspaceIdentifier) {
        return queryService.findProgress(workspaceIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.UPDATE)
    public void complete(AuthorizationContext context, String workspaceIdentifier) {
        commandService.complete(workspaceIdentifier);
    }
}
