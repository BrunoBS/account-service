package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import java.time.LocalDateTime;

public record EnvironmentOutput(
    Long version,
    String identifier,
    String workspaceIdentifier,
    String name,
    String description,
    String authorizationType,
    String environmentType,
    String parentIdentifier,
    Integer sortOrder,
    String authorizerGroup,
    String settings,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static EnvironmentOutput from(Environment e, String workspaceIdentifier) {
        return new EnvironmentOutput(
            e.getVersion(),
            e.getIdentifier(),
            workspaceIdentifier,
            e.getName(),
            e.getDescription(),
            e.getAuthorizationType().value(),
            e.getEnvironmentType().getCode(),
            e.getParent() == null ? null : e.getParent().getIdentifier(),
            e.getSortOrder(),
            e.getAuthorizerGroup(),
            e.getSettings(),
            e.getLifecycle().value(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
