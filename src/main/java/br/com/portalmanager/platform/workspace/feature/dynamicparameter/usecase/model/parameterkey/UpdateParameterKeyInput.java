package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey;

import tools.jackson.databind.JsonNode;

public record UpdateParameterKeyInput(
        Long version, String description, String type, JsonNode value) {}
