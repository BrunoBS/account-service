package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.participant;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.domain.participant.Participant;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.integration.ResourceSharingReferenceAdapter;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.repository.participant.ParticipantRepository;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantConfigurationInput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.contract.ContractAccessService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation.ResourceSharingValidator;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation.participant.ResourceSharingTransitionPolicy;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Aggregate locks serialize mutations; READ_COMMITTED keeps lazy mapping reads current after
 * waiting.
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ParticipantCommandService {

    private final EntityManager entityManager;
    private final ResourceSharingReferenceAdapter referenceResolver;
    private final ParticipantRepository participantRepository;
    private final ApplicationQueryService applicationQueryService;
    private final ResourceSharingValidator resourceSharingValidator;
    private final ResourceSharingTransitionPolicy transitionPolicy;
    private final EnvironmentMappingService environmentMappingService;
    private final ContractAccessService contractAccessService;

    public ParticipantCommandService(
            EntityManager entityManager,
            ResourceSharingReferenceAdapter referenceResolver,
            ParticipantRepository participantRepository,
            ApplicationQueryService applicationQueryService,
            ResourceSharingValidator resourceSharingValidator,
            ResourceSharingTransitionPolicy transitionPolicy,
            EnvironmentMappingService environmentMappingService,
            ContractAccessService contractAccessService) {
        this.entityManager = entityManager;
        this.referenceResolver = referenceResolver;
        this.participantRepository = participantRepository;
        this.applicationQueryService = applicationQueryService;
        this.resourceSharingValidator = resourceSharingValidator;
        this.transitionPolicy = transitionPolicy;
        this.environmentMappingService = environmentMappingService;
        this.contractAccessService = contractAccessService;
    }

    @Auditable(
            action = AuditAction.CREATE,
            event = "PARTICIPANT_REQUESTED",
            resourceType = "PARTICIPANT")
    public ParticipantOutput requestParticipant(
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String contractIdentifier) {
        ApplicationScopeReferenceOutput participantApplicationReference =
                applicationQueryService.findActiveScopeForUpdate(
                        participantWorkspaceIdentifier, participantApplicationIdentifier);
        Contract contract = contractAccessService.requiredAvailableContract(contractIdentifier);
        if (contract.getOwnerApplicationId()
                .equals(participantApplicationReference.applicationId())) {
            throw new ValidationException(ResourceSharingMessageKeys.SCOPE_INVALID);
        }
        Optional<Participant> existingParticipant =
                participantRepository.findByContractIdAndParticipantApplicationId(
                        contract.getId(), participantApplicationReference.applicationId());
        if (existingParticipant.isPresent()) {
            if (!existingParticipant
                    .get()
                    .getParticipantWorkspaceId()
                    .equals(participantApplicationReference.workspaceId())) {
                throw new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND);
            }
            throw new ValidationException(ResourceSharingMessageKeys.STATE_INVALID);
        }
        Participant participant =
                new Participant(
                        contract,
                        participantApplicationReference.workspaceId(),
                        participantApplicationReference.applicationId(),
                        LocalDateTime.now());
        return referenceResolver.participantOutput(participantRepository.saveAndFlush(participant));
    }

    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARTICIPANT_RESUBMITTED",
            resourceType = "PARTICIPANT")
    public ParticipantOutput requestParticipantAgain(
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String participantIdentifier) {
        ApplicationScopeReferenceOutput participantApplicationReference =
                applicationQueryService.findActiveScopeForUpdate(
                        participantWorkspaceIdentifier, participantApplicationIdentifier);
        Participant participant =
                findParticipantParticipant(participantApplicationReference, participantIdentifier);
        Contract contract = participant.getContract();
        contractAccessService.requireAvailableContract(contract);
        participant =
                requiredParticipantParticipant(
                        participantApplicationReference, participantIdentifier);
        entityManager.refresh(participant, LockModeType.PESSIMISTIC_WRITE);
        transitionPolicy.requireTransition(participant.getStatus(), ShareStatusTypeEnum.PENDING);
        participant.requestAgain(LocalDateTime.now());
        return referenceResolver.participantOutput(participantRepository.saveAndFlush(participant));
    }

    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARTICIPANT_APPROVED",
            resourceType = "PARTICIPANT")
    public ParticipantOutput approve(
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier,
            ParticipantConfigurationInput input) {
        Participant participant =
                requiredOwnerParticipant(
                        ownerWorkspaceIdentifier,
                        ownerApplicationIdentifier,
                        contractIdentifier,
                        participantIdentifier);
        transitionPolicy.requireTransition(participant.getStatus(), ShareStatusTypeEnum.APPROVED);
        applyConfiguration(ownerWorkspaceIdentifier, participant, input);
        participant.approve(participant.getPublicationMode(), LocalDateTime.now());
        return referenceResolver.participantOutput(participantRepository.saveAndFlush(participant));
    }

    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARTICIPANT_CONFIGURATION_UPDATED",
            resourceType = "PARTICIPANT")
    public ParticipantOutput updateConfiguration(
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier,
            ParticipantConfigurationInput input) {
        Participant participant =
                requiredOwnerParticipant(
                        ownerWorkspaceIdentifier,
                        ownerApplicationIdentifier,
                        contractIdentifier,
                        participantIdentifier);
        if (!ShareStatusTypeEnum.APPROVED.name().equals(participant.getStatus().value())) {
            throw new ValidationException(ResourceSharingMessageKeys.STATE_INVALID);
        }
        applyConfiguration(ownerWorkspaceIdentifier, participant, input);
        return referenceResolver.participantOutput(participantRepository.saveAndFlush(participant));
    }

    private void applyConfiguration(
            String ownerWorkspaceIdentifier,
            Participant participant,
            ParticipantConfigurationInput input) {
        contractAccessService.requireAvailableFeature(participant.getContract());
        resourceSharingValidator.validateConfiguration(input);
        String participantWorkspaceIdentifier =
                referenceResolver.workspaceIdentifier(participant.getParticipantWorkspaceId());
        applicationQueryService.findActiveScopeForShared(
                participant.getParticipantWorkspaceId(), participant.getParticipantApplicationId());
        LocalDateTime configurationTimestamp = LocalDateTime.now();
        participant.configure(
                PublicationModeTypeCode.of(input.publicationModeCode()),
                environmentMappingService.build(
                        ownerWorkspaceIdentifier,
                        participantWorkspaceIdentifier,
                        input.environmentMappings(),
                        configurationTimestamp),
                configurationTimestamp);
    }

    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARTICIPANT_REJECTED",
            resourceType = "PARTICIPANT")
    public ParticipantOutput reject(
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier) {
        Participant participant =
                requiredOwnerParticipant(
                        ownerWorkspaceIdentifier,
                        ownerApplicationIdentifier,
                        contractIdentifier,
                        participantIdentifier);
        transitionPolicy.requireTransition(participant.getStatus(), ShareStatusTypeEnum.REJECTED);
        participant.reject(LocalDateTime.now());
        return referenceResolver.participantOutput(participantRepository.saveAndFlush(participant));
    }

    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "PARTICIPANT_REVOKED",
            resourceType = "PARTICIPANT")
    public ParticipantOutput revoke(
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier) {
        Participant participant =
                requiredOwnerParticipant(
                        ownerWorkspaceIdentifier,
                        ownerApplicationIdentifier,
                        contractIdentifier,
                        participantIdentifier);
        transitionPolicy.requireTransition(participant.getStatus(), ShareStatusTypeEnum.REVOKED);
        participant.revoke(LocalDateTime.now());
        return referenceResolver.participantOutput(participantRepository.saveAndFlush(participant));
    }

    @Auditable(
            action = AuditAction.DELETE,
            event = "PARTICIPANT_DELETED",
            resourceType = "PARTICIPANT")
    public ParticipantOutput deleteParticipant(
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String participantIdentifier) {
        ApplicationScopeReferenceOutput participantApplicationReference =
                applicationQueryService.findActiveScopeForUpdate(
                        participantWorkspaceIdentifier, participantApplicationIdentifier);
        Participant participant =
                requiredParticipantParticipant(
                        participantApplicationReference, participantIdentifier);
        ParticipantOutput deletedParticipant = referenceResolver.participantOutput(participant);
        participantRepository.delete(participant);
        return deletedParticipant;
    }

    private Participant findParticipantParticipant(
            ApplicationScopeReferenceOutput participantApplication, String participantIdentifier) {
        return participantRepository
                .findByIdentifierAndParticipantWorkspaceIdAndParticipantApplicationId(
                        participantIdentifier,
                        participantApplication.workspaceId(),
                        participantApplication.applicationId())
                .orElseThrow(() -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
    }

    private Participant requiredParticipantParticipant(
            ApplicationScopeReferenceOutput participantApplication, String participantIdentifier) {
        return participantRepository
                .findParticipantParticipantForUpdate(
                        participantIdentifier,
                        participantApplication.workspaceId(),
                        participantApplication.applicationId())
                .orElseThrow(() -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
    }

    private Participant requiredOwnerParticipant(
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier) {
        Contract contract =
                contractAccessService.requiredActiveContract(
                        ownerWorkspaceIdentifier, ownerApplicationIdentifier, contractIdentifier);
        return participantRepository
                .findOwnerParticipantForUpdate(contract.getId(), participantIdentifier)
                .orElseThrow(() -> new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND));
    }
}
