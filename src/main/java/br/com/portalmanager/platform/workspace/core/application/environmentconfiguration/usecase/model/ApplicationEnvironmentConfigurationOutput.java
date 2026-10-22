package br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.model;


import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationEnvironmentConfigurationOutput(
        String identifier,
        String workspaceIdentifier,
        String applicationIdentifier,
        String environmentIdentifier,
        Long version,
        String lifecycle,
        JsonNode settings,
        List<PublisherOutput> publishers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
