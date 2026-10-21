package br.com.portalmanager.platform.workspace.feature.parameter.usecase.model;

import tools.jackson.databind.JsonNode;

/** Wire envelope shared with the read-only configuration and expression providers. */
public record PublishedParameterValue(String type, JsonNode value) {}
