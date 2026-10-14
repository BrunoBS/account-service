package br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record FeatureOutput(
    Long version, String identifier, String code, String name, String description,
    String microserviceIdentifier, String microserviceCode, String lifecycle,
    boolean shareable, JsonNode settings, LocalDateTime createdAt, LocalDateTime updatedAt
) {}
