package br.com.portalmanager.platform.workspace.core.environment.usecase.model;

import br.com.portalmanager.platform.library.authorization.resource.AuthorizableResource;
import br.com.portalmanager.platform.workspace.core.environment.domain.Environment;

import java.time.LocalDateTime;

public record EnvironmentOutput(Long version, String identifier, String workspaceIdentifier, String name,
                                String description, String authorizationType, String environmentType,
                                Integer sortOrder, String authorizerGroup, String settings, String lifecycle,
                                LocalDateTime createdAt, LocalDateTime updatedAt) implements AuthorizableResource {
    public static EnvironmentOutput from(Environment e, String workspaceIdentifier) {
        return new EnvironmentOutput(e.getVersion(), e.getIdentifier(), workspaceIdentifier, e.getName(),
                e.getDescription(), e.getAuthorizationType().value(), e.getEnvironmentType().value(), e.getSortOrder(),
                e.getAuthorizerGroup(), e.getSettings(), e.getLifecycle().value(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    @Override public String getAuthorizerGroup() { return authorizerGroup; }
}
