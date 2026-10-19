package br.com.portalmanager.platform.workspace.feature.sharing.usecase.operations.participant;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.participant.Participant;
import br.com.portalmanager.platform.workspace.feature.sharing.integration.ResourceSharingReferenceAdapter;
import br.com.portalmanager.platform.workspace.feature.sharing.repository.participant.ParticipantRepository;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.pagination.ResourceSharingPageInput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantPageOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.operations.contract.ContractAccessService;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.validation.ResourceSharingValidator;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.mapping.participant.ParticipantMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParticipantQueryService {

    private final ResourceSharingValidator resourceSharingValidator;
    private final ResourceSharingReferenceAdapter referenceResolver;
    private final ParticipantRepository participantRepository;
    private final ApplicationQueryService applicationQueryService;
    private final EnvironmentQueryService environmentQueryService;
    private final ContractAccessService contractAccessService;
    private final ParticipantMapper mapper;

    public ParticipantQueryService(
            ResourceSharingValidator resourceSharingValidator,
            ResourceSharingReferenceAdapter referenceResolver,
            ParticipantRepository participantRepository,
            ApplicationQueryService applicationQueryService,
            EnvironmentQueryService environmentQueryService,
            ContractAccessService contractAccessService,
            ParticipantMapper mapper) {
        this.resourceSharingValidator = resourceSharingValidator;
        this.referenceResolver = referenceResolver;
        this.participantRepository = participantRepository;
        this.applicationQueryService = applicationQueryService;
        this.environmentQueryService = environmentQueryService;
        this.contractAccessService = contractAccessService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ParticipantPageOutput listOwnerParticipants(
            String workspaceIdentifier,
            String applicationIdentifier,
            String contractIdentifier,
            String participantName,
            String participantApplicationIdentifier,
            String status,
            String page,
            String size) {
        ResourceSharingPageInput pagination = resourceSharingValidator.parsePage(page, size);
        resourceSharingValidator.validateOwnerParticipantFilters(
                status, participantApplicationIdentifier);
        Contract contract =
                contractAccessService.findOwnedContract(
                        workspaceIdentifier, applicationIdentifier, contractIdentifier);
        String normalizedParticipantName =
                participantName == null || participantName.isBlank()
                        ? null
                        : participantName.trim().toLowerCase(Locale.ROOT);
        Page<Long> participantIds =
                participantRepository.findOwnerParticipantIds(
                        contract.getId(),
                        status,
                        participantApplicationIdentifier,
                        normalizedParticipantName,
                        PageRequest.of(pagination.page(), pagination.size()));
        return participantPageOutput(participantIds);
    }

    @Transactional(readOnly = true)
    public ParticipantOutput findOwnerParticipant(
            String workspaceIdentifier,
            String applicationIdentifier,
            String contractIdentifier,
            String participantIdentifier) {
        return referenceResolver.participantOutput(
                findOwnedParticipant(
                        workspaceIdentifier,
                        applicationIdentifier,
                        contractIdentifier,
                        participantIdentifier));
    }

    @Transactional(readOnly = true)
    public ParticipantOutput findOwnerParticipantDetails(
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier) {
        Participant participant =
                findOwnedParticipant(
                        ownerWorkspaceIdentifier,
                        ownerApplicationIdentifier,
                        contractIdentifier,
                        participantIdentifier);
        ParticipantOutput output = referenceResolver.participantOutput(participant);
        if (!canListSourceEnvironments(participant)) {
            return mapper.withSourceEnvironments(output, List.of());
        }
        return mapper.withSourceEnvironments(
                output,
                environmentQueryService.listActiveForShared(
                        output.participantWorkspaceIdentifier()));
    }

    private boolean canListSourceEnvironments(Participant participant) {
        Contract contract = participant.getContract();
        return contract.isActive()
                && !applicationQueryService
                        .findActiveReferencesForResourceSharing(
                                List.of(participant.getParticipantApplicationId()))
                        .isEmpty();
    }

    private Participant findOwnedParticipant(
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier) {
        Contract contract =
                contractAccessService.findOwnedContract(
                        ownerWorkspaceIdentifier, ownerApplicationIdentifier, contractIdentifier);
        Participant participant =
                participantRepository
                        .findByContractIdAndIdentifier(contract.getId(), participantIdentifier)
                        .orElseThrow(
                                () -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
        contractAccessService.requireFeatureForDetails(contract);
        return participant;
    }

    @Transactional(readOnly = true)
    public ParticipantPageOutput listParticipantParticipants(
            String workspaceIdentifier,
            String applicationIdentifier,
            String status,
            String contractIdentifier,
            String page,
            String size) {
        ResourceSharingPageInput pagination = resourceSharingValidator.parsePage(page, size);
        resourceSharingValidator.validateParticipantFilters(status, contractIdentifier);
        ApplicationScopeReferenceOutput applicationReference =
                applicationQueryService.findActiveScope(workspaceIdentifier, applicationIdentifier);
        Page<Long> participantIds =
                participantRepository.findParticipantParticipantIds(
                        applicationReference.workspaceId(),
                        applicationReference.applicationId(),
                        status,
                        contractIdentifier,
                        PageRequest.of(pagination.page(), pagination.size()));
        return participantPageOutput(participantIds);
    }

    private ParticipantPageOutput participantPageOutput(Page<Long> participantIds) {
        if (participantIds.isEmpty()) {
            return mapper.toPageOutput(participantIds, List.of());
        }
        Map<Long, Participant> participantsById =
                participantRepository.findByIdIn(participantIds.getContent()).stream()
                        .collect(Collectors.toMap(Participant::getId, participant -> participant));
        List<Participant> orderedParticipants =
                participantIds.getContent().stream().map(participantsById::get).toList();
        return mapper.toPageOutput(
                participantIds, referenceResolver.participantOutputs(orderedParticipants));
    }

    @Transactional(readOnly = true)
    public ParticipantOutput findParticipantParticipant(
            String workspaceIdentifier,
            String applicationIdentifier,
            String participantIdentifier) {
        ApplicationScopeReferenceOutput applicationReference =
                applicationQueryService.findActiveScope(workspaceIdentifier, applicationIdentifier);
        Participant participant =
                participantRepository
                        .findByIdentifierAndParticipantWorkspaceIdAndParticipantApplicationId(
                                participantIdentifier,
                                applicationReference.workspaceId(),
                                applicationReference.applicationId())
                        .orElseThrow(
                                () -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
        contractAccessService.requireFeatureForDetails(participant.getContract());
        return referenceResolver.participantOutput(participant);
    }
}
