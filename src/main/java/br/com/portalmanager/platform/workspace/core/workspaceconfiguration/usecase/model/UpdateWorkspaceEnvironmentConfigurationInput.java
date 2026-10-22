package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record UpdateWorkspaceEnvironmentConfigurationInput(
        Long version, JsonNode settings, List<PublisherInput> publishers) {}
