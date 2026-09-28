package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentTypeCompatibility;

public record EnvironmentCompatibilityOutput(String identifier, String parentTypeCode, String childTypeCode,
                                             String lifecycle) {
    public static EnvironmentCompatibilityOutput from(EnvironmentTypeCompatibility value) {
        return new EnvironmentCompatibilityOutput(value.getIdentifier(), value.getParentType().getCode(),
                value.getChildType().getCode(), value.getLifecycle());
    }
}
