package br.com.portalmanager.platform.workspace.feature.resourcesharing.integration;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.participant.Participant;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractSummaryOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureQueryService;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class ResourceSharingReferenceAdapter {

    private final FeatureQueryService featureQueryService;
    private final WorkspaceQueryService workspaceQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final EnvironmentQueryService environmentQueryService;

    public ResourceSharingReferenceAdapter(
            FeatureQueryService featureQueryService,
            WorkspaceQueryService workspaceQueryService,
            ApplicationQueryService applicationQueryService,
            EnvironmentQueryService environmentQueryService) {
        this.featureQueryService = featureQueryService;
        this.workspaceQueryService = workspaceQueryService;
        this.applicationQueryService = applicationQueryService;
        this.environmentQueryService = environmentQueryService;
    }

    public Long shareableFeatureId(String identifier) {
        return featureQueryService.findShareableInternalIdByIdentifier(identifier);
    }

    public String featureIdentifier(Long featureId) {
        return featureQueryService.findIdentifierByInternalId(featureId);
    }

    public boolean isFeatureAvailable(Contract contract) {
        return featureQueryService
                .findAvailableIdentifiersForResourceSharing(
                        java.util.List.of(contract.getFeatureId()))
                .containsKey(contract.getFeatureId());
    }

    public String workspaceIdentifier(Long id) {
        return workspaceQueryService.findReferenceIdentifierForResourceSharing(id);
    }

    public String applicationIdentifier(Long workspaceId, Long id) {
        return applicationQueryService.findIdentifierByInternalId(workspaceId, id);
    }

    public ContractOutput contractOutput(Contract contract) {
        return contractOutput(
                contract,
                workspaceIdentifier(contract.getOwnerWorkspaceId()),
                applicationIdentifier(
                        contract.getOwnerWorkspaceId(), contract.getOwnerApplicationId()));
    }

    public ContractOutput contractOutput(
            Contract contract, String workspaceIdentifier, String applicationIdentifier) {
        FeatureReferenceOutput featureReference =
                featureQueryService.findReferenceByInternalId(contract.getFeatureId());
        return ContractOutput.from(
                contract,
                workspaceIdentifier,
                applicationIdentifier,
                featureReference.identifier(),
                featureReference.name());
    }

    public List<ContractOutput> contractOutputs(List<Contract> contracts) {
        if (contracts.isEmpty()) return List.of();
        Map<Long, ApplicationReferenceOutput> ownerApplications =
                applicationQueryService.findReferencesForResourceSharing(
                        contracts.stream()
                                .map(Contract::getOwnerApplicationId)
                                .distinct()
                                .toList());
        Map<Long, FeatureReferenceOutput> featureReferences =
                featureQueryService.findReferencesByInternalIds(
                        contracts.stream().map(Contract::getFeatureId).distinct().toList());
        return contracts.stream()
                .map(
                        contract -> {
                            ApplicationReferenceOutput ownerApplication =
                                    requiredApplicationReference(
                                            ownerApplications,
                                            contract.getOwnerApplicationId(),
                                            contract.getOwnerWorkspaceId());
                            FeatureReferenceOutput featureReference =
                                    featureReferences.get(contract.getFeatureId());
                            return ContractOutput.from(
                                    contract,
                                    ownerApplication.workspaceIdentifier(),
                                    ownerApplication.identifier(),
                                    featureReference.identifier(),
                                    featureReference.name());
                        })
                .toList();
    }

    public ParticipantOutput participantOutput(Participant participant) {
        return participantOutputs(List.of(participant)).getFirst();
    }

    public List<ParticipantOutput> participantOutputs(List<Participant> participants) {
        if (participants.isEmpty()) return List.of();
        Map<Long, ApplicationReferenceOutput> applicationReferences =
                applicationQueryService.findReferencesForResourceSharing(
                        participants.stream()
                                .flatMap(
                                        participant ->
                                                Stream.of(
                                                        participant.getParticipantApplicationId(),
                                                        participant
                                                                .getContract()
                                                                .getOwnerApplicationId()))
                                .distinct()
                                .toList());
        Map<Long, FeatureReferenceOutput> featureReferences =
                featureQueryService.findReferencesByInternalIds(
                        participants.stream()
                                .map(participant -> participant.getContract().getFeatureId())
                                .distinct()
                                .toList());
        Map<Long, String> environmentIdentifiers =
                environmentQueryService.findIdentifiersByInternalIds(
                        participants.stream()
                                .flatMap(participant -> participant.getMappings().stream())
                                .flatMap(
                                        mapping ->
                                                Stream.of(
                                                        mapping.getSourceEnvironmentId(),
                                                        mapping.getDestinationEnvironmentId()))
                                .distinct()
                                .toList());
        return participants.stream()
                .map(
                        participant ->
                                participantOutput(
                                        participant,
                                        applicationReferences,
                                        featureReferences,
                                        environmentIdentifiers))
                .toList();
    }

    private ParticipantOutput participantOutput(
            Participant participant,
            Map<Long, ApplicationReferenceOutput> applicationReferences,
            Map<Long, FeatureReferenceOutput> featureReferences,
            Map<Long, String> environmentIdentifiers) {
        Contract contract = participant.getContract();
        ApplicationReferenceOutput participantApplication =
                requiredApplicationReference(
                        applicationReferences,
                        participant.getParticipantApplicationId(),
                        participant.getParticipantWorkspaceId());
        ApplicationReferenceOutput ownerApplication =
                requiredApplicationReference(
                        applicationReferences,
                        contract.getOwnerApplicationId(),
                        contract.getOwnerWorkspaceId());
        FeatureReferenceOutput featureReference = featureReferences.get(contract.getFeatureId());
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
        return ParticipantOutput.from(
                participant,
                participantApplication.workspaceIdentifier(),
                participantApplication.identifier(),
                mappings,
                new ContractSummaryOutput(
                        contract.getIdentifier(),
                        featureReference.name(),
                        contract.getLifecycle().value(),
                        ownerApplication.workspaceIdentifier(),
                        ownerApplication.identifier(),
                        featureReference.identifier()));
    }

    private ApplicationReferenceOutput requiredApplicationReference(
            Map<Long, ApplicationReferenceOutput> applicationReferences,
            Long applicationId,
            Long workspaceId) {
        ApplicationReferenceOutput reference = applicationReferences.get(applicationId);
        if (reference == null || !workspaceId.equals(reference.workspaceId())) {
            throw new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND);
        }
        return reference;
    }
}
