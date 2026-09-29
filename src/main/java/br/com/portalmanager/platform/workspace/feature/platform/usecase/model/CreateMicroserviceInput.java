package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

public record CreateMicroserviceInput(String code, String name, String description, String settings) {
    public CreateMicroserviceInput(String code, String name, String description) {
        this(code, name, description, "{}");
    }
}
