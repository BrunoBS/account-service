package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record FeatureOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String microserviceIdentifier,
        String microserviceCode,
        String lifecycle,
        boolean shareable,
        JsonNode settings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
