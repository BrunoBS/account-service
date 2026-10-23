package br.com.portalmanager.platform.workspace.feature.sharing.integration;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.participant.Participant;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.mapping.contract.ContractMapper;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.mapping.participant.ParticipantMapper;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractSummaryOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.EnvironmentReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.FeatureReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.validation.SharingReferenceGuard;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ResourceSharingReferenceAdapter implements SharingReferencePort {

    private final SharingReferenceGuard guard;
    private final FeatureQueryService featureQueries;
    private final WorkspaceQueryService workspaceQueries;
    private final ApplicationQueryService applicationQueries;
    private final EnvironmentQueryService environmentQueries;
    private final ContractMapper contractMapper;
    private final ParticipantMapper participantMapper;

    public ResourceSharingReferenceAdapter(
            FeatureQueryService featureQueries,
            WorkspaceQueryService workspaceQueries,
            ApplicationQueryService applicationQueries,
            EnvironmentQueryService environmentQueries,
            ContractMapper contractMapper,
            ParticipantMapper participantMapper,
            SharingReferenceGuard guard) {
        this.guard = guard;
        this.featureQueries = featureQueries;
        this.workspaceQueries = workspaceQueries;
        this.applicationQueries = applicationQueries;
        this.environmentQueries = environmentQueries;
        this.contractMapper = contractMapper;
        this.participantMapper = participantMapper;
    }

    public Long shareableFeatureId(String identifier) {
        return findShareableInternalIdByIdentifier(identifier);
    }

    public String featureIdentifier(Long featureId) {
        return findIdentifierByInternalId(featureId);
    }

    public boolean isFeatureAvailable(Contract contract) {
        return findAvailableIdentifiersForResourceSharing(List.of(contract.getFeatureId()))
                .containsKey(contract.getFeatureId());
    }

    public String workspaceIdentifier(Long id) {
        return findReferenceIdentifierForResourceSharing(id);
    }

    public String applicationIdentifier(Long workspaceId, Long id) {
        return findIdentifierByInternalId(workspaceId, id);
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
                findReferenceByInternalId(contract.getFeatureId());
        return contractMapper.toOutput(
                contract, workspaceIdentifier, applicationIdentifier, featureReference);
    }

    public List<ContractOutput> contractOutputs(List<Contract> contracts) {
        if (contracts.isEmpty()) return List.of();
        Map<Long, ApplicationReferenceOutput> ownerApplications =
                findReferencesForResourceSharing(
                        contracts.stream()
                                .map(Contract::getOwnerApplicationId)
                                .distinct()
                                .toList());
        Map<Long, FeatureReferenceOutput> featureReferences =
                findReferencesByInternalIds(
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
                            return contractMapper.toOutput(
                                    contract,
                                    ownerApplication.workspaceIdentifier(),
                                    ownerApplication.identifier(),
                                    featureReference);
                        })
                .toList();
    }

    public ParticipantOutput participantOutput(Participant participant) {
        return participantOutputs(List.of(participant)).getFirst();
    }

    public List<ParticipantOutput> participantOutputs(List<Participant> participants) {
        if (participants.isEmpty()) return List.of();
        Map<Long, ApplicationReferenceOutput> applicationReferences =
                findReferencesForResourceSharing(
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
                findReferencesByInternalIds(
                        participants.stream()
                                .map(participant -> participant.getContract().getFeatureId())
                                .distinct()
                                .toList());
        Map<Long, String> environmentIdentifiers =
                findIdentifiersByInternalIds(
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
        ContractSummaryOutput contractSummary =
                contractMapper.toSummary(contract, ownerApplication, featureReference);
        return participantMapper.toOutput(
                participant, participantApplication, contractSummary, environmentIdentifiers);
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

    @Override
    public ApplicationScopeReferenceOutput findActiveScope(
            String workspaceIdentifier, String identifier) {
        requireContext();
        return toApplicationScopeReferenceOutput(
                applicationQueries.findActiveScope(workspaceIdentifier, identifier));
    }

    @Override
    public ApplicationScopeReferenceOutput findActiveScopeForUpdate(
            String workspaceIdentifier, String identifier) {
        requireContext();
        return toApplicationScopeReferenceOutput(
                applicationQueries.findActiveScopeForUpdate(workspaceIdentifier, identifier));
    }

    @Override
    public ApplicationScopeReferenceOutput findActiveScopeForShared(
            Long workspaceId, Long applicationId) {
        guard.requireApplication(workspaceId, applicationId);
        return toApplicationScopeReferenceOutput(
                applicationQueries.findActiveScopeForShared(workspaceId, applicationId));
    }

    @Override
    public String findIdentifierByInternalId(Long workspaceId, Long applicationId) {
        guard.requireApplication(workspaceId, applicationId);
        return applicationQueries.findIdentifierByInternalId(workspaceId, applicationId);
    }

    @Override
    public Map<Long, ApplicationReferenceOutput> findReferencesForResourceSharing(
            Collection<Long> applicationIds) {
        Map<Long, Set<Long>> links = guard.requireApplications(applicationIds);
        Map<Long, ApplicationReferenceOutput> references =
                applicationQueries
                        .findReferencesForResourceSharing(applicationIds)
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> toApplicationReferenceOutput(entry.getValue())));
        references
                .values()
                .forEach(
                        reference ->
                                guard.requireResolvedApplication(
                                        links, reference.workspaceId(), reference.id()));
        return references;
    }

    @Override
    public List<EnvironmentOutput> listActiveForShared(
            Long workspaceId, String workspaceIdentifier) {
        requireWorkspaceIdentifier(workspaceId, workspaceIdentifier);
        return environmentQueries.listActiveForShared(workspaceIdentifier).stream()
                .map(this::toEnvironmentOutput)
                .toList();
    }

    @Override
    public Map<String, EnvironmentReferenceOutput> findAvailableReferencesForResourceSharing(
            Long workspaceId, String workspaceIdentifier, Collection<String> identifiers) {
        requireWorkspaceIdentifier(workspaceId, workspaceIdentifier);
        return environmentQueries
                .findAvailableReferencesForResourceSharing(workspaceIdentifier, identifiers)
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> toEnvironmentReferenceOutput(entry.getValue())));
    }

    @Override
    public Map<Long, String> findIdentifiersByInternalIds(Collection<Long> environmentIds) {
        guard.requireEnvironmentMappings(environmentIds);
        return environmentQueries.findIdentifiersByInternalIds(environmentIds);
    }

    @Override
    public Long findShareableInternalIdByIdentifier(String identifier) {
        requireContext();
        return featureQueries.findShareableInternalIdByIdentifier(identifier);
    }

    @Override
    public String findIdentifierByInternalId(Long id) {
        requireContext();
        return featureQueries.findIdentifierByInternalId(id);
    }

    @Override
    public FeatureReferenceOutput findReferenceByInternalId(Long id) {
        requireContext();
        return toFeatureReferenceOutput(featureQueries.findReferenceByInternalId(id));
    }

    @Override
    public Map<Long, FeatureReferenceOutput> findReferencesByInternalIds(Collection<Long> ids) {
        requireContext();
        return featureQueries.findReferencesByInternalIds(ids).entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> toFeatureReferenceOutput(entry.getValue())));
    }

    @Override
    public Map<Long, String> findAvailableIdentifiersForResourceSharing(Collection<Long> ids) {
        requireContext();
        return featureQueries.findAvailableIdentifiersForResourceSharing(ids);
    }

    @Override
    public String findReferenceIdentifierForResourceSharing(Long id) {
        guard.requireWorkspace(id);
        return workspaceQueries.findReferenceIdentifierForResourceSharing(id);
    }

    private ApplicationReferenceOutput toApplicationReferenceOutput(
            br.com.portalmanager.platform.workspace.core.application.usecase.model
                            .ApplicationReferenceOutput
                    value) {
        return new ApplicationReferenceOutput(
                value.id(),
                value.workspaceId(),
                value.identifier(),
                value.workspaceIdentifier(),
                value.name());
    }

    private ApplicationScopeReferenceOutput toApplicationScopeReferenceOutput(
            br.com.portalmanager.platform.workspace.core.application.usecase.model
                            .ApplicationScopeReferenceOutput
                    value) {
        return new ApplicationScopeReferenceOutput(
                value.workspaceId(), value.applicationId(), value.applicationScope());
    }

    private EnvironmentOutput toEnvironmentOutput(
            br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment
                            .EnvironmentOutput
                    value) {
        return new EnvironmentOutput(
                value.version(),
                value.identifier(),
                value.workspaceIdentifier(),
                value.name(),
                value.description(),
                value.authorizationType(),
                value.environmentTypeCode(),
                value.parentIdentifier(),
                value.sortOrder(),
                value.authorizerGroup(),
                value.settings(),
                value.lifecycle(),
                value.createdAt(),
                value.updatedAt());
    }

    private EnvironmentReferenceOutput toEnvironmentReferenceOutput(
            br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment
                            .EnvironmentReferenceOutput
                    value) {
        return new EnvironmentReferenceOutput(value.id(), toEnvironmentOutput(value.environment()));
    }

    private FeatureReferenceOutput toFeatureReferenceOutput(
            br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature
                            .FeatureReferenceOutput
                    value) {
        return new FeatureReferenceOutput(value.identifier(), value.name());
    }

    private void requireContext() {
        guard.requireContext();
    }

    private void requireWorkspaceIdentifier(Long workspaceId, String identifier) {
        guard.requireWorkspace(workspaceId);
        if (!Objects.equals(
                identifier,
                workspaceQueries.findReferenceIdentifierForResourceSharing(workspaceId)))
            throw new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND);
    }

    @Override
    public List<ApplicationReferenceOutput> findActiveReferencesForResourceSharing(
            Collection<Long> ids) {
        Map<Long, Set<Long>> links = guard.requireApplications(ids);
        List<ApplicationReferenceOutput> references =
                applicationQueries.findActiveReferencesForResourceSharing(ids).stream()
                        .map(this::toApplicationReferenceOutput)
                        .toList();
        references.forEach(
                reference ->
                        guard.requireResolvedApplication(
                                links, reference.workspaceId(), reference.id()));
        return references;
    }
}
