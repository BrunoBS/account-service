package br.com.portalmanager.platform.workspace.entrypoint.web.onboarding.response;

import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.OnboardingPhaseOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.OnboardingPhaseStatus;
import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record OnboardingPhaseResponse(
    String code,
    String label,
    String description,
    Integer sortOrder,
    String orientation,
    JsonNode settings,
    OnboardingPhaseStatus status,
    String requester,
    LocalDateTime completedAt
) {

    public static OnboardingPhaseResponse from(OnboardingPhaseOutput output) {
        return new OnboardingPhaseResponse(
            output.code(),
            output.label(),
            output.description(),
            output.sortOrder(),
            output.orientation(),
            output.settings(),
            output.status(),
            output.requester(),
            output.completedAt()
        );
    }
}
