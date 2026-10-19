package br.com.portalmanager.platform.workspace.feature.parameter.usecase.model;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record ParameterKeyOutput(
        Long version,
        String identifier,
        String microserviceIdentifier,
        String key,
        String fullKey,
        String description,
        String type,
        JsonNode value,
        String status,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
