package br.com.portalmanager.platform.workspace.feature.shared.usecase.model;

import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedParticipation;
import java.time.LocalDateTime;
import java.util.List;

public record SharedParticipationOutput(
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

    public static SharedParticipationOutput from(SharedParticipation p) {
        return new SharedParticipationOutput(
            p.getIdentifier(),
            p.getContract().getIdentifier(),
            p.getParticipantWorkspaceIdentifier(),
            p.getParticipantApplicationIdentifier(),
            p.getStatus().value(),
            p.getPublicationMode() == null ? null : p.getPublicationMode().value(),
            p.getCreatedAt(),
            p.getUpdatedAt(),
            p
                .getMappings()
                .stream()
                .map(m ->
                    new EnvironmentMapping(m.getSourceEnvironmentIdentifier(), m.getDestinationEnvironmentIdentifier())
                )
                .toList()
        );
    }
}
