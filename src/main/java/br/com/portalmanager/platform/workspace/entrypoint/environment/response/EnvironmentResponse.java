package br.com.portalmanager.platform.workspace.entrypoint.environment.response;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentOutput;
import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record EnvironmentResponse(
    Long version,
    String identifier,
    String workspaceIdentifier,
    String name,
    String description,
    String authorizationType,
    String environmentTypeCode,
    String parentIdentifier,
    Integer sortOrder,
    String authorizerGroup,
    JsonNode settings,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static EnvironmentResponse from(EnvironmentOutput output) {
        return new EnvironmentResponse(
            output.version(),
            output.identifier(),
            output.workspaceIdentifier(),
            output.name(),
            output.description(),
            output.authorizationType(),
            output.environmentTypeCode(),
            output.parentIdentifier(),
            output.sortOrder(),
            output.authorizerGroup(),
            output.settings(),
            output.lifecycle(),
            output.createdAt(),
            output.updatedAt()
        );
    }

}
