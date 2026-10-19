package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.mapping;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.workspace.feature.onboarding.domain.WorkspaceOnboardingCompletion;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.phase.OnboardingPhaseOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.phase.OnboardingPhaseStatus;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.progress.OnboardingProgressOutput;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class OnboardingProgressMapper {

    public OnboardingProgressOutput toOutput(
            String workspaceIdentifier,
            String workspaceType,
            boolean completed,
            boolean readyToComplete,
            List<OnboardingPhaseOutput> phases) {
        return new OnboardingProgressOutput(
                workspaceIdentifier, workspaceType, completed, readyToComplete, phases);
    }

    public OnboardingPhaseOutput toPhaseOutput(
            CatalogDTO phase,
            WorkspaceOnboardingCompletion completion,
            OnboardingPhaseStatus status) {
        JsonNode orientation = phase.settings().get("orientation");
        return new OnboardingPhaseOutput(
                phase.code(), phase.label(), phase.description(), phase.sortOrder(),
                orientation == null || orientation.isNull() ? null : orientation.asString(),
                phase.settings(), status,
                completion == null ? null : completion.getRequester(),
                completion == null ? null : completion.getCompletedAt());
    }
}
