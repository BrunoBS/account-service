package br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.model;

import tools.jackson.databind.JsonNode;

public record PublisherOutput(String publisherIdentifier, Integer sortOrder, JsonNode parameters) {}
