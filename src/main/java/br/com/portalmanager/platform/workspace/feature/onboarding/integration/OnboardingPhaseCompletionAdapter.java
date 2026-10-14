package br.com.portalmanager.platform.workspace.feature.onboarding.integration;

import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.OnboardingPhaseCompletionCommandService;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.OnboardingPhaseCompletionPort;
import org.springframework.stereotype.Component;

@Component
public class OnboardingPhaseCompletionAdapter implements OnboardingPhaseCompletionPort {

    private final OnboardingPhaseCompletionCommandService phaseCompletionCommandService;

    public OnboardingPhaseCompletionAdapter(OnboardingPhaseCompletionCommandService phaseCompletionCommandService) {
        this.phaseCompletionCommandService = phaseCompletionCommandService;
    }

    @Override
    public void registerPhaseCompletion(Long workspaceId, OnboardingPhaseTypeCode phaseCode) {
        phaseCompletionCommandService.registerPhaseCompletion(workspaceId, phaseCode);
    }
}
