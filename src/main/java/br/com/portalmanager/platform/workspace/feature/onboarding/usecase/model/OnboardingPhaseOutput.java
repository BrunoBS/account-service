package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record OnboardingPhaseOutput(
    String code,
    String label,
    String description,
    Integer sortOrder,
    String orientation,
    JsonNode settings,
    OnboardingPhaseStatus status,
    String requester,
    LocalDateTime completedAt
) {}
