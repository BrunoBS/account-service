package br.com.portalmanager.platform.workspace.entrypoint.web.shared.request;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.ParticipationApprovalInput;

public record ParticipationApprovalRequest(String publicationModeCode, EnvironmentMappingRequest environmentMappings) {
    public ParticipationApprovalInput toInput() {
        return new ParticipationApprovalInput(
            publicationModeCode,
            environmentMappings == null ? null : environmentMappings.toInput()
        );
    }
}
