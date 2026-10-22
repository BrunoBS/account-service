package br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model;


import tools.jackson.databind.JsonNode;

import java.util.List;

public record CreateWorkspaceEnvironmentConfigurationInput(
        Long version,
        String environmentIdentifier,
        JsonNode settings,
        List<PublisherInput> publishers) {}
