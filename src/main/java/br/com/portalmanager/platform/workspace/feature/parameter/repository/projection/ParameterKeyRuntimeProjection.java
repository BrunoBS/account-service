package br.com.portalmanager.platform.workspace.feature.parameter.repository.projection;

import tools.jackson.databind.JsonNode;

public interface ParameterKeyRuntimeProjection {
    Long getId();

    Long getMicroserviceId();

    String getKey();

    String getType();

    JsonNode getValue();
}
