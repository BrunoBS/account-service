package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model;

import tools.jackson.databind.JsonNode;

public record ResolvedPublisher(
        Long publisherId, String publisherIdentifier, Integer sortOrder, JsonNode parameters) {}
