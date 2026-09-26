package br.com.portalmanager.platform.workspace.entrypoint.web.platform.service.response;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.ServiceOutput;

import java.time.LocalDateTime;

public record ServiceResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ServiceResponse from(ServiceOutput output) {
        return new ServiceResponse(
                output.version(),
                output.identifier(),
                output.code(),
                output.name(),
                output.description(),
                output.lifecycle(),
                output.createdAt(),
                output.updatedAt()
        );
    }
}
