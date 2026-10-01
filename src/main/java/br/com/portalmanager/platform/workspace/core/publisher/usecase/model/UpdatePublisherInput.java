package br.com.portalmanager.platform.workspace.core.publisher.usecase.model;

import tools.jackson.databind.JsonNode;

public record UpdatePublisherInput(Long version, String name, String description, String scope,
                                   Boolean deprecated, JsonNode settings) {}
