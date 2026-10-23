package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.progress;

import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.progress.OnboardingProgressOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.WorkspaceOnboardingPort;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.OnboardingValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingCommandService {

    private final OnboardingQueryService queryService;
    private final WorkspaceOnboardingPort workspaces;
    private final OnboardingValidator validator;

    public OnboardingCommandService(
            OnboardingQueryService queryService,
            WorkspaceOnboardingPort workspaces,
            OnboardingValidator validator) {
        this.queryService = queryService;
        this.workspaces = workspaces;
        this.validator = validator;
    }

    @Transactional
    public void complete(String workspaceIdentifier) {
        OnboardingProgressOutput progress = queryService.findProgress(workspaceIdentifier);
        validator.requireReadyToComplete(progress);
        workspaces.completeOnboarding(workspaceIdentifier);
    }
}
