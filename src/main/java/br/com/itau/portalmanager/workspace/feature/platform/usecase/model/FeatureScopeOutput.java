package br.com.itau.portalmanager.workspace.feature.platform.usecase.model;

import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType;

public record FeatureScopeOutput(
        String code,
        String label,
        boolean active
) {
    public static FeatureScopeOutput from(FeatureScopeType scope) {
        return new FeatureScopeOutput(scope.getCode(), scope.getLabel(), scope.isActive());
    }
}
