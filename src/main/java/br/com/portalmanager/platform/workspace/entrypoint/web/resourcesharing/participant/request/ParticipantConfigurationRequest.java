package br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.participant.request;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantConfigurationInput;

public record ParticipantConfigurationRequest(
        String publicationModeCode, EnvironmentMappingRequest environmentMappings) {
    public ParticipantConfigurationInput toInput() {
        return new ParticipantConfigurationInput(
                publicationModeCode,
                environmentMappings == null ? null : environmentMappings.toInput());
    }
}
