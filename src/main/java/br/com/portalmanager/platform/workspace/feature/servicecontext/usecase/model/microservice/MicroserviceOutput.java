package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record MicroserviceOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        JsonNode settings,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
