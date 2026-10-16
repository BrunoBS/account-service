package br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.participant.request;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantStatusInput;

public record ParticipantStatusRequest(
        String action, String publicationModeCode, EnvironmentMappingRequest environmentMappings) {
    public ParticipantStatusInput toInput() {
        return new ParticipantStatusInput(
                action,
                publicationModeCode,
                environmentMappings == null ? null : environmentMappings.toInput());
    }
}
