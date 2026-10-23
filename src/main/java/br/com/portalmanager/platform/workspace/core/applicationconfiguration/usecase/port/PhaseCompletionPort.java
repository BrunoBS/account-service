package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port;

import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;

/** Records a phase once, preserving its first requester and completion date. */
@FunctionalInterface
public interface PhaseCompletionPort {

    void registerPhaseCompletion(Long workspaceId, OnboardingPhaseTypeCode phaseCode);
}
