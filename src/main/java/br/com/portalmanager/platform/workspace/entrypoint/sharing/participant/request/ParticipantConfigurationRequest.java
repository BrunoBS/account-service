package br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request;

import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantConfigurationInput;

public record ParticipantConfigurationRequest(
        String publicationModeCode, EnvironmentMappingRequest environmentMappings) {
    public ParticipantConfigurationInput toInput() {
        return new ParticipantConfigurationInput(
                publicationModeCode,
                environmentMappings == null ? null : environmentMappings.toInput());
    }
}
