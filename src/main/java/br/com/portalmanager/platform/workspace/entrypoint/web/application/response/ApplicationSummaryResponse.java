package br.com.portalmanager.platform.workspace.entrypoint.web.application.response;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationSummary;

public record ApplicationSummaryResponse(
    String identifier,
    String name,
    String alias,
    String acronym,
    String applicationScope,
    String authorizerGroup
) {
    public static ApplicationSummaryResponse from(ApplicationSummary summary) {
        return new ApplicationSummaryResponse(
            summary.identifier(),
            summary.name(),
            summary.alias(),
            summary.acronym(),
            summary.applicationScope(),
            summary.authorizerGroup()
        );
    }
}
