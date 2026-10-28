package br.com.portalmanager.platform.workspace.feature.parameter.repository.projection;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public interface ParameterKeyReadProjection {
    Long getId();

    Long getVersion();

    String getIdentifier();

    Long getMicroserviceId();

    String getKey();

    String getDescription();

    String getType();

    JsonNode getValue();

    String getStatus();

    boolean isActive();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
