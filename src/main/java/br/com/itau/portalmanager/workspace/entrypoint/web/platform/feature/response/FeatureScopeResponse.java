package br.com.itau.portalmanager.workspace.entrypoint.web.platform.feature.response;

import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.FeatureScopeOutput;

public record FeatureScopeResponse(
        String code,
        String label,
        boolean active
) {
    public static FeatureScopeResponse from(FeatureScopeOutput output) {
        return new FeatureScopeResponse(output.code(), output.label(), output.active());
    }
}
