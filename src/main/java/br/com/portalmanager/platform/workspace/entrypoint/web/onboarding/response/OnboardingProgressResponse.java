package br.com.portalmanager.platform.workspace.entrypoint.web.onboarding.response;

import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.progress.OnboardingProgressOutput;
import java.util.List;

public record OnboardingProgressResponse(
    String workspaceIdentifier,
    String workspaceType,
    boolean completed,
    boolean readyToComplete,
    List<OnboardingPhaseResponse> phases
) {

    public static OnboardingProgressResponse from(OnboardingProgressOutput output) {
        return new OnboardingProgressResponse(
            output.workspaceIdentifier(),
            output.workspaceType(),
            output.completed(),
            output.readyToComplete(),
            output.phases().stream().map(OnboardingPhaseResponse::from).toList()
        );
    }
}
