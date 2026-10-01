package br.com.portalmanager.platform.workspace.core.publisher.usecase.model;

import tools.jackson.databind.JsonNode;

public record CreatePublisherInput(String code, String name, String description, String scope,
                                   Boolean deprecated, JsonNode settings) {}
