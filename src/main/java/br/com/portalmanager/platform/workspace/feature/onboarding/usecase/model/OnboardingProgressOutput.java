package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model;

import java.util.List;

public record OnboardingProgressOutput(
    String workspaceIdentifier,
    String workspaceType,
    boolean completed,
    boolean readyToComplete,
    List<OnboardingPhaseOutput> phases
) {}
