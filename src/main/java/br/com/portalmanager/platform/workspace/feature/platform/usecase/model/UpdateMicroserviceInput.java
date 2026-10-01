package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import tools.jackson.databind.JsonNode;

public record UpdateMicroserviceInput(
        String name,
        String description,
        JsonNode settings
) {
}
