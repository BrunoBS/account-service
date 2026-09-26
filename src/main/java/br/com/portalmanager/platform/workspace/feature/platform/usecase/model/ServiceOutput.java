package br.com.portalmanager.platform.workspace.feature.platform.usecase.model;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Service;

import java.time.LocalDateTime;

public record ServiceOutput(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ServiceOutput from(Service service) {
        return new ServiceOutput(
                service.getVersion(),
                service.getIdentifier(),
                service.getCode(),
                service.getName(),
                service.getDescription(),
                service.getLifecycle().toString(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
