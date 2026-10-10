package br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.feature.shared.domain.participation.SharedParticipation;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractSummaryOutput;
import java.time.LocalDateTime;
import java.util.List;

public record SharedParticipationOutput(
    String identifier,
    String contractIdentifier,
    SharedContractSummaryOutput contract,
    String participantWorkspaceIdentifier,
    String participantApplicationIdentifier,
    String status,
    String publicationMode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<EnvironmentMapping> mappings,
    List<EnvironmentOutput> sourceEnvironments
) {
    public SharedParticipationOutput withSourceEnvironments(List<EnvironmentOutput> sourceEnvironments) {
        return new SharedParticipationOutput(
            identifier,
            contractIdentifier,
            contract,
            participantWorkspaceIdentifier,
            participantApplicationIdentifier,
            status,
            publicationMode,
            createdAt,
            updatedAt,
            mappings,
            sourceEnvironments
        );
    }

    public record EnvironmentMapping(String sourceEnvironmentIdentifier, String destinationEnvironmentIdentifier) {}

    public static SharedParticipationOutput from(
        SharedParticipation participation,
        String workspaceIdentifier,
        String applicationIdentifier,
        List<EnvironmentMapping> mappings,
        SharedContractSummaryOutput contract
    ) {
        return new SharedParticipationOutput(
            participation.getIdentifier(),
            participation.getContract().getIdentifier(),
            contract,
            workspaceIdentifier,
            applicationIdentifier,
            participation.getStatus().value(),
            participation.getPublicationMode() == null ? null : participation.getPublicationMode().value(),
            participation.getCreatedAt(),
            participation.getUpdatedAt(),
            mappings,
            null
        );
    }
}
