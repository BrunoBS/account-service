package br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record MicroserviceOutput(
    Long version,
    String identifier,
    String code,
    String name,
    String description,
    JsonNode settings,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
