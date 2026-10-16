package br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.projection;

import tools.jackson.databind.JsonNode;

public interface ParameterKeyRuntimeProjection {
    Long getId();

    String getIdentifier();

    Long getMicroserviceId();

    String getKey();

    boolean isActive();

    String getType();

    JsonNode getValue();
}
