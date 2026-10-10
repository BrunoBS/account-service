package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import java.util.List;

public record UpdateFeatureContextInput(String name, String description, List<String> featureIdentifiers) {
    public UpdateFeatureContextInput(String name, String description) {
        this(name, description, null);
    }
}
