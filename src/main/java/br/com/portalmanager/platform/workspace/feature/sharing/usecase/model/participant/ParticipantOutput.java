package br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant;

import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractSummaryOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.EnvironmentOutput;

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
    public record EnvironmentMapping(
            String sourceEnvironmentIdentifier, String destinationEnvironmentIdentifier) {}
}
