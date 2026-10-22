package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model;

import tools.jackson.databind.JsonNode;

public record PublisherInput(String publisherCode, Integer sortOrder, JsonNode parameters) {}
