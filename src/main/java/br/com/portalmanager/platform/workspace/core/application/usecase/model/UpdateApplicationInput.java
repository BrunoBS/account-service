package br.com.portalmanager.platform.workspace.core.application.usecase.model;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record UpdateApplicationInput(Long version, String name, String alias, String acronym,
                                     String applicationScope, String authorizerGroup, JsonNode settings,
                                     List<String> tags) {}
