package br.com.portalmanager.platform.workspace.entrypoint.web.shared.request;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationStatusInput;

public record ParticipationStatusRequest(
    String action,
    String publicationModeCode,
    EnvironmentMappingRequest environmentMappings
) {
    public ParticipationStatusInput toInput() {
        return new ParticipationStatusInput(
            action,
            publicationModeCode,
            environmentMappings == null ? null : environmentMappings.toInput()
        );
    }
}
