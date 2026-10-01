package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

import tools.jackson.databind.JsonNode;

public record UpdateEnvironmentInput(Long version, String name, String description, String authorizationType,
                                     Integer sortOrder, String authorizerGroup, JsonNode settings,
                                     String environmentTypeCode) {}
