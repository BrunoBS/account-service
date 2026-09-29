package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;

import java.time.LocalDateTime;

public record MicroserviceOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String settings,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MicroserviceOutput from(Microservice microservice) {
        return new MicroserviceOutput(
                microservice.getVersion(), microservice.getIdentifier(), microservice.getCode(),
                microservice.getName(), microservice.getDescription(), microservice.getSettings(),
                microservice.getLifecycle().toString(), microservice.getCreatedAt(), microservice.getUpdatedAt()
        );
    }
}
