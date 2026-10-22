package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record CreateApplicationEnvironmentConfigurationInput(
        Long version,
        String environmentIdentifier,
        JsonNode settings,
        List<PublisherInput> publishers,
        boolean copyWorkspacePublishers) {}
