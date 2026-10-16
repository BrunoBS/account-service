package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup;

public record CreateFeatureGroupInput(Long version, String code, String name, String description) {
    public CreateFeatureGroupInput(String code, String name, String description) {
        this(null, code, name, description);
    }
}
