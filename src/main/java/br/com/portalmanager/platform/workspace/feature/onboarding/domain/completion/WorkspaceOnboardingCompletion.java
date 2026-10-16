package br.com.portalmanager.platform.workspace.feature.onboarding.domain.completion;

import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_onboarding_completions")
public class WorkspaceOnboardingCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "onboarding_phase_code", nullable = false, length = 50))
    private OnboardingPhaseTypeCode phase;

    @Column(name = "requester", nullable = false, length = 10)
    private String requester;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    protected WorkspaceOnboardingCompletion() {}

    public WorkspaceOnboardingCompletion(
            Long workspaceId,
            OnboardingPhaseTypeCode phase,
            String requester,
            LocalDateTime completedAt) {
        this.workspaceId = workspaceId;
        this.phase = phase;
        this.requester = requester;
        this.completedAt = completedAt;
    }

    public String getPhaseCode() {
        return phase.value();
    }

    public String getRequester() {
        return requester;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
