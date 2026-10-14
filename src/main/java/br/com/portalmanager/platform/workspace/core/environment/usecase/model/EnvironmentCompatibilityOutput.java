package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeCompatibility;

public record EnvironmentCompatibilityOutput(
    String identifier,
    String parentTypeCode,
    String childTypeCode,
    String lifecycle
) {
    public static EnvironmentCompatibilityOutput from(EnvironmentTypeCompatibility compatibility) {
        return new EnvironmentCompatibilityOutput(
            compatibility.getIdentifier(),
            compatibility.getParentType().getCode(),
            compatibility.getChildType().getCode(),
            compatibility.getLifecycle().value()
        );
    }
}
