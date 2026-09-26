package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.response;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.MicroserviceOutput;

import java.time.LocalDateTime;

public record MicroserviceResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MicroserviceResponse from(MicroserviceOutput output) {
        return new MicroserviceResponse(
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
