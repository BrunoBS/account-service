package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record EnvironmentOutput(
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
    public static EnvironmentOutput from(Environment environment, String workspaceIdentifier) {
        String parentIdentifier = environment.getParent() == null ? null : environment.getParent().getIdentifier();
        return from(environment, workspaceIdentifier, parentIdentifier);
    }

    public static EnvironmentOutput from(Environment environment, String workspaceIdentifier, String parentIdentifier) {
        return new EnvironmentOutput(
            environment.getVersion(),
            environment.getIdentifier(),
            workspaceIdentifier,
            environment.getName(),
            environment.getDescription(),
            environment.getAuthorizationType().value(),
            environment.getEnvironmentType().getCode(),
            parentIdentifier,
            environment.getSortOrder(),
            environment.getAuthorizerGroup(),
            environment.getSettings(),
            environment.getLifecycle().value(),
            environment.getCreatedAt(),
            environment.getUpdatedAt()
        );
    }
}
