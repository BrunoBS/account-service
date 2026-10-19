package br.com.portalmanager.platform.workspace.feature.parameter.usecase.model;

import java.time.LocalDateTime;

/** Command output and audit snapshot intentionally contain no parameter values. */
public record ParameterKeyMetadata(
        Long version,
        String identifier,
        String microserviceIdentifier,
        String key,
        String fullKey,
        String description,
        String type,
        String status,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
