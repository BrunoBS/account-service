package br.com.portalmanager.platform.workspace.feature.parameter.usecase.model;

import tools.jackson.databind.JsonNode;

public record UpdateParameterKeyInput(
        Long version, String description, String type, JsonNode value) {}
