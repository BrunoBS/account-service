package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.participant.EnvironmentMapping;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.participant.Participant;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractSummaryOutput;

import java.time.LocalDateTime;
import java.util.List;

public record ParticipantOutput(
        String identifier,
        String contractIdentifier,
        ContractSummaryOutput contract,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String status,
        String publicationMode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<EnvironmentMapping> mappings,
        List<EnvironmentOutput> sourceEnvironments) {
    public ParticipantOutput withSourceEnvironments(List<EnvironmentOutput> sourceEnvironments) {
        return new ParticipantOutput(
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
                sourceEnvironments);
    }

    public record EnvironmentMapping(
            String sourceEnvironmentIdentifier, String destinationEnvironmentIdentifier) {}

    public static ParticipantOutput from(
            Participant participant,
            String workspaceIdentifier,
            String applicationIdentifier,
            List<EnvironmentMapping> mappings,
            ContractSummaryOutput contract) {
        return new ParticipantOutput(
                participant.getIdentifier(),
                participant.getContract().getIdentifier(),
                contract,
                workspaceIdentifier,
                applicationIdentifier,
                participant.getStatus().value(),
                participant.getPublicationMode() == null
                        ? null
                        : participant.getPublicationMode().value(),
                participant.getCreatedAt(),
                participant.getUpdatedAt(),
                mappings,
                null);
    }
}
