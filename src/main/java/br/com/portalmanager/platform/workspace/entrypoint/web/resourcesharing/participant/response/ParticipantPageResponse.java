package br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.participant.response;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantPageOutput;

import java.util.List;

public record ParticipantPageResponse(
        List<ParticipantResponse> content, int page, int size, long totalElements, int totalPages) {
    public static ParticipantPageResponse from(ParticipantPageOutput output) {
        return new ParticipantPageResponse(
                output.content().stream().map(ParticipantResponse::from).toList(),
                output.page(),
                output.size(),
                output.totalElements(),
                output.totalPages());
    }
}
