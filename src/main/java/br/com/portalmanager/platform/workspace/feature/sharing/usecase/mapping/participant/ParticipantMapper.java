package br.com.portalmanager.platform.workspace.feature.sharing.usecase.mapping.participant;

import br.com.portalmanager.platform.workspace.feature.sharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.participant.EnvironmentMapping;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.participant.Participant;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractSummaryOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantPageOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ParticipantMapper {

    public Participant toDomain(
            Contract contract, Long workspaceId, Long applicationId, LocalDateTime now) {
        return new Participant(contract, workspaceId, applicationId, now);
    }

    public EnvironmentMapping toDomain(
            Long sourceEnvironmentId, Long destinationEnvironmentId, LocalDateTime now) {
        return new EnvironmentMapping(sourceEnvironmentId, destinationEnvironmentId, now);
    }

    public PublicationModeTypeCode toPublicationMode(String code) {
        return PublicationModeTypeCode.of(code);
    }

    public ParticipantOutput toOutput(
            Participant participant,
            ApplicationReferenceOutput participantApplication,
            ContractSummaryOutput contract,
            Map<Long, String> environmentIdentifiers) {
        List<ParticipantOutput.EnvironmentMapping> mappings =
                participant.getMappings().stream()
                        .map(
                                mapping ->
                                        new ParticipantOutput.EnvironmentMapping(
                                                environmentIdentifiers.get(
                                                        mapping.getSourceEnvironmentId()),
                                                environmentIdentifiers.get(
                                                        mapping.getDestinationEnvironmentId())))
                        .toList();
        Contract parent = participant.getContract();
        return new ParticipantOutput(
                participant.getIdentifier(),
                parent.getIdentifier(),
                contract,
                participantApplication.workspaceIdentifier(),
                participantApplication.identifier(),
                participant.getStatus().value(),
                participant.getPublicationMode() == null
                        ? null
                        : participant.getPublicationMode().value(),
                participant.getCreatedAt(),
                participant.getUpdatedAt(),
                mappings,
                null);
    }

    public ParticipantOutput withSourceEnvironments(
            ParticipantOutput output, List<EnvironmentOutput> sourceEnvironments) {
        return new ParticipantOutput(
                output.identifier(),
                output.contractIdentifier(),
                output.contract(),
                output.participantWorkspaceIdentifier(),
                output.participantApplicationIdentifier(),
                output.status(),
                output.publicationMode(),
                output.createdAt(),
                output.updatedAt(),
                output.mappings(),
                sourceEnvironments);
    }

    public ParticipantPageOutput toPageOutput(Page<Long> page, List<ParticipantOutput> content) {
        return new ParticipantPageOutput(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
