package br.com.portalmanager.platform.workspace.core.publisher.usecase.model;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record PublisherOutput(
    Long version,
    String identifier,
    String code,
    String name,
    String description,
    String scope,
    boolean deprecated,
    JsonNode settings,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
