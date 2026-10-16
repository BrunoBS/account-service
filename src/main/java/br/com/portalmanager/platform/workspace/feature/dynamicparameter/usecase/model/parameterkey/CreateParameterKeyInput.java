package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey;

import tools.jackson.databind.JsonNode;

public record CreateParameterKeyInput(
        Long version,
        String microserviceIdentifier,
        String key,
        String description,
        String type,
        JsonNode value) {}
