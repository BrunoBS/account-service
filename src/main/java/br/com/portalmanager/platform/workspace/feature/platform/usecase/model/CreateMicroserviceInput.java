package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import tools.jackson.databind.JsonNode;

public record CreateMicroserviceInput(String code, String name, String description, JsonNode settings) {
    public CreateMicroserviceInput(String code, String name, String description) {
        this(code, name, description, tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode());
    }
}
