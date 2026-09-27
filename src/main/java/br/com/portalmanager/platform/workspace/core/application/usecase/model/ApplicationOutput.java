package br.com.portalmanager.platform.workspace.core.application.usecase.model;

import br.com.portalmanager.platform.library.authorization.resource.AuthorizableResource;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationOutput(Long version, String identifier, String workspaceIdentifier, String name,
                                String alias, String acronym, String applicationScope, String authorizerGroup,
                                String settings, String lifecycle,
                                LocalDateTime createdAt, LocalDateTime updatedAt, List<String> tags)
        implements AuthorizableResource {
    public static ApplicationOutput from(Application application, String workspaceIdentifier, List<String> tags) {
        return new ApplicationOutput(application.getVersion(), application.getIdentifier(),
                workspaceIdentifier, application.getName(), application.getAlias(),
                application.getAcronym(), application.getApplicationScope().value(), application.getAuthorizerGroup(),
                application.getSettings(), application.getLifecycle().value(),
                application.getCreatedAt(), application.getUpdatedAt(), tags == null ? List.of() : List.copyOf(tags));
    }

    @Override public String getAuthorizerGroup() { return authorizerGroup; }
}
