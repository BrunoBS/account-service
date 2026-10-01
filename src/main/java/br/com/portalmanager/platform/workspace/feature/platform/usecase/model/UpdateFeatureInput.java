package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import tools.jackson.databind.JsonNode;

public record UpdateFeatureInput(
        String name,
        String description,
        String microserviceIdentifier,
        JsonNode settings
) {
}
