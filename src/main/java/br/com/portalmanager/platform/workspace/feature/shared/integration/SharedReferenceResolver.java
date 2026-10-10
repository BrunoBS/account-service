package br.com.portalmanager.platform.workspace.feature.shared.integration;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.domain.participation.SharedParticipation;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractSummaryOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationOutput;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class SharedReferenceResolver {

    private final FeatureQueryService featureQueryService;
    private final WorkspaceQueryService workspaceQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final EnvironmentQueryService environmentQueryService;

    public SharedReferenceResolver(
        FeatureQueryService featureQueryService,
        WorkspaceQueryService workspaceQueryService,
        ApplicationQueryService applicationQueryService,
        EnvironmentQueryService environmentQueryService
    ) {
        this.featureQueryService = featureQueryService;
        this.workspaceQueryService = workspaceQueryService;
        this.applicationQueryService = applicationQueryService;
        this.environmentQueryService = environmentQueryService;
    }

    public Long shareableFeatureId(String identifier) {
        return featureQueryService.findShareableInternalId(identifier);
    }

    public String featureIdentifier(Long featureId) {
        return featureQueryService.findIdentifierByInternalId(featureId);
    }

    public boolean isFeatureAvailable(SharedContract contract) {
        return featureQueryService
            .findAvailableIdentifiersForShared(java.util.List.of(contract.getFeatureId()))
            .containsKey(contract.getFeatureId());
    }

    public String workspaceIdentifier(Long id) {
        return workspaceQueryService.findReferenceIdentifierByInternalId(id);
    }

    public String applicationIdentifier(Long workspaceId, Long id) {
        return applicationQueryService.findIdentifierByInternalId(workspaceId, id);
    }

    public SharedContractOutput contractOutput(SharedContract contract) {
        return contractOutput(
            contract,
            workspaceIdentifier(contract.getOwnerWorkspaceId()),
            applicationIdentifier(contract.getOwnerWorkspaceId(), contract.getOwnerApplicationId())
        );
    }

    public SharedContractOutput contractOutput(
        SharedContract contract,
        String workspaceIdentifier,
        String applicationIdentifier
    ) {
        FeatureReferenceOutput featureReference = featureQueryService.findReferenceByInternalId(
            contract.getFeatureId()
        );
        return SharedContractOutput.from(
            contract,
            workspaceIdentifier,
            applicationIdentifier,
            featureReference.identifier(),
            featureReference.name()
        );
    }

    public List<SharedContractOutput> contractOutputs(List<SharedContract> contracts) {
        if (contracts.isEmpty()) return List.of();
        Map<Long, ApplicationReferenceOutput> ownerApplications = applicationQueryService.findReferencesForShared(
            contracts.stream().map(SharedContract::getOwnerApplicationId).distinct().toList()
        );
        Map<Long, FeatureReferenceOutput> featureReferences = featureQueryService.findReferencesByInternalIds(
            contracts.stream().map(SharedContract::getFeatureId).distinct().toList()
        );
        return contracts
            .stream()
            .map(contract -> {
                ApplicationReferenceOutput ownerApplication = requiredApplicationReference(
                    ownerApplications,
                    contract.getOwnerApplicationId(),
                    contract.getOwnerWorkspaceId()
                );
                FeatureReferenceOutput featureReference = featureReferences.get(contract.getFeatureId());
                return SharedContractOutput.from(
                    contract,
                    ownerApplication.workspaceIdentifier(),
                    ownerApplication.identifier(),
                    featureReference.identifier(),
                    featureReference.name()
                );
            })
            .toList();
    }

    public SharedParticipationOutput participationOutput(SharedParticipation participation) {
        return participationOutputs(List.of(participation)).getFirst();
    }

    public List<SharedParticipationOutput> participationOutputs(List<SharedParticipation> participations) {
        if (participations.isEmpty()) return List.of();
        Map<Long, ApplicationReferenceOutput> applicationReferences = applicationQueryService.findReferencesForShared(
            participations
                .stream()
                .flatMap(participation ->
                    Stream.of(
                        participation.getParticipantApplicationId(),
                        participation.getContract().getOwnerApplicationId()
                    )
                )
                .distinct()
                .toList()
        );
        Map<Long, FeatureReferenceOutput> featureReferences = featureQueryService.findReferencesByInternalIds(
            participations
                .stream()
                .map(participation -> participation.getContract().getFeatureId())
                .distinct()
                .toList()
        );
        Map<Long, String> environmentIdentifiers = environmentQueryService.findIdentifiersByInternalIds(
            participations
                .stream()
                .flatMap(participation -> participation.getMappings().stream())
                .flatMap(mapping -> Stream.of(mapping.getSourceEnvironmentId(), mapping.getDestinationEnvironmentId()))
                .distinct()
                .toList()
        );
        return participations
            .stream()
            .map(participation ->
                participationOutput(participation, applicationReferences, featureReferences, environmentIdentifiers)
            )
            .toList();
    }

    private SharedParticipationOutput participationOutput(
        SharedParticipation participation,
        Map<Long, ApplicationReferenceOutput> applicationReferences,
        Map<Long, FeatureReferenceOutput> featureReferences,
        Map<Long, String> environmentIdentifiers
    ) {
        SharedContract contract = participation.getContract();
        ApplicationReferenceOutput participantApplication = requiredApplicationReference(
            applicationReferences,
            participation.getParticipantApplicationId(),
            participation.getParticipantWorkspaceId()
        );
        ApplicationReferenceOutput ownerApplication = requiredApplicationReference(
            applicationReferences,
            contract.getOwnerApplicationId(),
            contract.getOwnerWorkspaceId()
        );
        FeatureReferenceOutput featureReference = featureReferences.get(contract.getFeatureId());
        List<SharedParticipationOutput.EnvironmentMapping> mappings = participation
            .getMappings()
            .stream()
            .map(mapping ->
                new SharedParticipationOutput.EnvironmentMapping(
                    environmentIdentifiers.get(mapping.getSourceEnvironmentId()),
                    environmentIdentifiers.get(mapping.getDestinationEnvironmentId())
                )
            )
            .toList();
        return SharedParticipationOutput.from(
            participation,
            participantApplication.workspaceIdentifier(),
            participantApplication.identifier(),
            mappings,
            new SharedContractSummaryOutput(
                contract.getIdentifier(),
                featureReference.name(),
                contract.getLifecycle().value(),
                ownerApplication.workspaceIdentifier(),
                ownerApplication.identifier(),
                featureReference.identifier()
            )
        );
    }

    private ApplicationReferenceOutput requiredApplicationReference(
        Map<Long, ApplicationReferenceOutput> applicationReferences,
        Long applicationId,
        Long workspaceId
    ) {
        ApplicationReferenceOutput reference = applicationReferences.get(applicationId);
        if (reference == null || !workspaceId.equals(reference.workspaceId())) {
            throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
        }
        return reference;
    }
}
