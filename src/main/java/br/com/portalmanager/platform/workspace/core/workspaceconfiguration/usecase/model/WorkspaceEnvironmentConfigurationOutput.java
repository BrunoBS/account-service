package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceEnvironmentConfigurationOutput(
        String identifier,
        String workspaceIdentifier,
        String environmentIdentifier,
        Long version,
        String lifecycle,
        JsonNode settings,
        List<PublisherOutput> publishers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
