package br.com.portalmanager.platform.workspace.entrypoint.web.shared.response;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedParticipationOutput;
import java.time.LocalDateTime;
import java.util.List;

public record SharedParticipationResponse(
    String identifier,
    String contractIdentifier,
    String participantWorkspaceIdentifier,
    String participantApplicationIdentifier,
    String status,
    String publicationMode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<EnvironmentMapping> mappings
) {
    public record EnvironmentMapping(String sourceEnvironmentIdentifier, String destinationEnvironmentIdentifier) {}

    public static SharedParticipationResponse from(SharedParticipationOutput o) {
        return new SharedParticipationResponse(
            o.identifier(),
            o.contractIdentifier(),
            o.participantWorkspaceIdentifier(),
            o.participantApplicationIdentifier(),
            o.status(),
            o.publicationMode(),
            o.createdAt(),
            o.updatedAt(),
            o
                .mappings()
                .stream()
                .map(m -> new EnvironmentMapping(m.sourceEnvironmentIdentifier(), m.destinationEnvironmentIdentifier()))
                .toList()
        );
    }
}
