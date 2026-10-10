package br.com.portalmanager.platform.workspace.core.application.usecase.model;

import java.util.List;
import tools.jackson.databind.JsonNode;

public record CreateApplicationInput(
    String name,
    String alias,
    String acronym,
    String applicationScope,
    String authorizerGroup,
    JsonNode settings,
    List<String> tags
) {}
