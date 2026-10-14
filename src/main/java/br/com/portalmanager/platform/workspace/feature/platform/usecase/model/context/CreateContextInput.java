package br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context;

public record CreateContextInput(Long version, String code, String name, String description) {
    public CreateContextInput(String code, String name, String description) {
        this(null, code, name, description);
    }
}
