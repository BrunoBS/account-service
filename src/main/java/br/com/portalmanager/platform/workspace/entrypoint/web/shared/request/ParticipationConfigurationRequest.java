package br.com.portalmanager.platform.workspace.entrypoint.web.shared.request;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationConfigurationInput;

public record ParticipationConfigurationRequest(
    String publicationModeCode,
    EnvironmentMappingRequest environmentMappings
) {
    public ParticipationConfigurationInput toInput() {
        return new ParticipationConfigurationInput(
            publicationModeCode,
            environmentMappings == null ? null : environmentMappings.toInput()
        );
    }
}
