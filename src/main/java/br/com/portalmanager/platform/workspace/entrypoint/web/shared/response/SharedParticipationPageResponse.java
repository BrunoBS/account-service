package br.com.portalmanager.platform.workspace.entrypoint.web.shared.response;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationPageOutput;
import java.util.List;

public record SharedParticipationPageResponse(
    List<SharedParticipationResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static SharedParticipationPageResponse from(SharedParticipationPageOutput output) {
        return new SharedParticipationPageResponse(
            output.content().stream().map(SharedParticipationResponse::from).toList(),
            output.page(),
            output.size(),
            output.totalElements(),
            output.totalPages()
        );
    }
}
