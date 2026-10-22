package br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model;

import tools.jackson.databind.JsonNode;

public record PublisherInput(String publisherIdentifier, Integer sortOrder, JsonNode parameters) {}
