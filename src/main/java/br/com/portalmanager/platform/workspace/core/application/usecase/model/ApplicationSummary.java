package br.com.portalmanager.platform.workspace.core.application.usecase.model;

import br.com.portalmanager.platform.library.authorization.resource.AuthorizableResource;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;

public record ApplicationSummary(String identifier, String name, String alias, String acronym,
                                 String applicationScope, String authorizerGroup)
        implements AuthorizableResource {
    public static ApplicationSummary from(Application application) {
        return new ApplicationSummary(application.getIdentifier(), application.getName(), application.getAlias(),
                application.getAcronym(), application.getApplicationScope().value(),
                application.getAuthorizerGroup());
    }

    @Override public String getAuthorizerGroup() { return authorizerGroup; }
}
