package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import java.util.List;

public record CreateFeatureContextInput(String code, String name, String description, List<String> featureIdentifiers) {
    public CreateFeatureContextInput(String code, String name, String description) {
        this(code, name, description, null);
    }
}
