package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.completion;

import br.com.portalmanager.platform.workspace.feature.onboarding.repository.WorkspaceOnboardingCompletionRepository;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.OnboardingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OnboardingPhaseCompletionCommandService {

    private final WorkspaceOnboardingCompletionRepository completionRepository;
    private final OnboardingValidator validator;

    public OnboardingPhaseCompletionCommandService(
            WorkspaceOnboardingCompletionRepository completionRepository,
            OnboardingValidator validator) {
        this.completionRepository = completionRepository;
        this.validator = validator;
    }

    @Transactional
    public void registerPhaseCompletion(Long workspaceId, OnboardingPhaseTypeCode phaseCode) {
        String activePhaseCode = validator.requireActivePhase(phaseCode);
        String requester = validator.requireAuthenticatedRequester();
        completionRepository.insertIfAbsent(
                workspaceId, activePhaseCode, requester, LocalDateTime.now());
    }
}
