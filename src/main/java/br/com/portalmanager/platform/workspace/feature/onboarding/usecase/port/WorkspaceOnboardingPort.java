package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port;

public interface WorkspaceOnboardingPort {
    WorkspaceOnboardingReference findOnboardingReference(String identifier);

    void completeOnboarding(String identifier);

    record WorkspaceOnboardingReference(Long id, String workspaceType, boolean onboarding) {}
}
