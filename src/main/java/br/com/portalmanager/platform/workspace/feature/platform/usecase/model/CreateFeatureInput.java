package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import tools.jackson.databind.JsonNode;

public record CreateFeatureInput(
        String code,
        String name,
        String description,
        String microserviceIdentifier,
        JsonNode settings
) {
}
