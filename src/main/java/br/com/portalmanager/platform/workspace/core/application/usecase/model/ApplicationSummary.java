package br.com.portalmanager.platform.workspace.core.application.usecase.model;

import br.com.portalmanager.platform.workspace.core.application.domain.Application;

public record ApplicationSummary(String identifier, String name, String alias, String acronym,
                                 String applicationScope, String authorizerGroup) {
    public static ApplicationSummary from(Application application) {
        return new ApplicationSummary(application.getIdentifier(), application.getName(), application.getAlias(),
                application.getAcronym(), application.getApplicationScope().value(),
                application.getAuthorizerGroup());
    }
}
