package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.phase;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record OnboardingPhaseOutput(
        String code,
        String label,
        String description,
        Integer sortOrder,
        String orientation,
        JsonNode settings,
        OnboardingPhaseStatus status,
        String requester,
        LocalDateTime completedAt) {}
