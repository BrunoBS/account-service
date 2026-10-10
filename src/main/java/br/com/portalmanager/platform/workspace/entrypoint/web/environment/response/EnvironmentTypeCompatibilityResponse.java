package br.com.portalmanager.platform.workspace.entrypoint.web.environment.response;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;

public record EnvironmentTypeCompatibilityResponse(
    String identifier,
    String parentTypeCode,
    String childTypeCode,
    String lifecycle
) {
    public static EnvironmentTypeCompatibilityResponse from(EnvironmentCompatibilityOutput output) {
        return new EnvironmentTypeCompatibilityResponse(
            output.identifier(),
            output.parentTypeCode(),
            output.childTypeCode(),
            output.lifecycle()
        );
    }
}
