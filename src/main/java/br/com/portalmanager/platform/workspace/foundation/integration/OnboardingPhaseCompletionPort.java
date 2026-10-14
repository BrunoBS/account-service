package br.com.portalmanager.platform.workspace.foundation.integration;

import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;

/** Records a phase once, preserving its first requester and completion date. */
@FunctionalInterface
public interface OnboardingPhaseCompletionPort {

    void registerPhaseCompletion(Long workspaceId, OnboardingPhaseTypeCode phaseCode);
}
