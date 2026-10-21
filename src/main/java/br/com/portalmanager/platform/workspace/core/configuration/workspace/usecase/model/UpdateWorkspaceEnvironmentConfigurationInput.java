package br.com.portalmanager.platform.workspace.core.configuration.workspace.usecase.model;

import br.com.portalmanager.platform.workspace.core.configuration.model.PublisherInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record UpdateWorkspaceEnvironmentConfigurationInput(
        Long version, JsonNode settings, List<PublisherInput> publishers) {}
