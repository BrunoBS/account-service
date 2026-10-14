package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceCommandService;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.OnboardingProgressOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.OnboardingValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingCommandService {

    private final OnboardingQueryService queryService;
    private final WorkspaceCommandService workspaceCommandService;
    private final OnboardingValidator validator;

    public OnboardingCommandService(
        OnboardingQueryService queryService,
        WorkspaceCommandService workspaceCommandService,
        OnboardingValidator validator
    ) {
        this.queryService = queryService;
        this.workspaceCommandService = workspaceCommandService;
        this.validator = validator;
    }

    @Transactional
    public void complete(String workspaceIdentifier) {
        OnboardingProgressOutput progress = queryService.findProgress(workspaceIdentifier);
        validator.requireReadyToComplete(progress);
        workspaceCommandService.completeOnboarding(workspaceIdentifier);
    }
}
