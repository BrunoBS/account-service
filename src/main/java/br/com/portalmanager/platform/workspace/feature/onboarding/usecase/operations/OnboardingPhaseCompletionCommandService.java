package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations;

import br.com.portalmanager.platform.workspace.feature.onboarding.repository.WorkspaceOnboardingCompletionRepository;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.OnboardingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingPhaseCompletionCommandService {

    private final WorkspaceOnboardingCompletionRepository completionRepository;
    private final OnboardingValidator validator;

    public OnboardingPhaseCompletionCommandService(
        WorkspaceOnboardingCompletionRepository completionRepository,
        OnboardingValidator validator
    ) {
        this.completionRepository = completionRepository;
        this.validator = validator;
    }

    @Transactional
    public void registerPhaseCompletion(Long workspaceId, OnboardingPhaseTypeCode phaseCode) {
        String activePhaseCode = validator.requireActivePhase(phaseCode);
        String requester = validator.requireAuthenticatedRequester();
        completionRepository.insertIfAbsent(workspaceId, activePhaseCode, requester, LocalDateTime.now());
    }
}
