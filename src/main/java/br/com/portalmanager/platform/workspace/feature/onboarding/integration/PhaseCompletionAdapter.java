package br.com.portalmanager.platform.workspace.feature.onboarding.integration;

import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.completion.OnboardingPhaseCompletionCommandService;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.PhaseCompletionPort;

import org.springframework.stereotype.Component;

@Component
public class PhaseCompletionAdapter implements PhaseCompletionPort {

    private final OnboardingPhaseCompletionCommandService phaseCompletionCommandService;

    public PhaseCompletionAdapter(
            OnboardingPhaseCompletionCommandService phaseCompletionCommandService) {
        this.phaseCompletionCommandService = phaseCompletionCommandService;
    }

    @Override
    public void registerPhaseCompletion(Long workspaceId, OnboardingPhaseTypeCode phaseCode) {
        phaseCompletionCommandService.registerPhaseCompletion(workspaceId, phaseCode);
    }
}
