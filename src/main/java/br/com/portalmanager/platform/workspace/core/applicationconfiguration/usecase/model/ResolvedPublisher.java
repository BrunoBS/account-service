package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model;

import tools.jackson.databind.JsonNode;

public record ResolvedPublisher(String publisherCode, Integer sortOrder, JsonNode parameters) {}
