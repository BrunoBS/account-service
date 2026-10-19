package br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request;

import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantStatusInput;

public record ParticipantStatusRequest(
        String action, String publicationModeCode, EnvironmentMappingRequest environmentMappings) {
    public ParticipantStatusInput toInput() {
        return new ParticipantStatusInput(
                action,
                publicationModeCode,
                environmentMappings == null ? null : environmentMappings.toInput());
    }
}
