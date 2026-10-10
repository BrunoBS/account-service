package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.participation;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedMessageKeys;
import br.com.portalmanager.platform.workspace.feature.shared.domain.contract.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.domain.participation.SharedParticipation;
import br.com.portalmanager.platform.workspace.feature.shared.integration.SharedReferenceResolver;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedParticipationRepository;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationConfigurationInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.contract.SharedContractAccessService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.mapping.SharedEnvironmentMappingService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedTransitionPolicy;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Aggregate locks serialize mutations; READ_COMMITTED keeps lazy mapping reads current after waiting. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class SharedParticipationCommandService {

    private final EntityManager entityManager;
    private final SharedReferenceResolver referenceResolver;
    private final SharedParticipationRepository participationRepository;
    private final ApplicationQueryService applicationQueryService;
    private final SharedValidator sharedValidator;
    private final SharedTransitionPolicy transitionPolicy;
    private final SharedEnvironmentMappingService environmentMappingService;
    private final SharedContractAccessService contractAccessService;

    public SharedParticipationCommandService(
        EntityManager entityManager,
        SharedReferenceResolver referenceResolver,
        SharedParticipationRepository participationRepository,
        ApplicationQueryService applicationQueryService,
        SharedValidator sharedValidator,
        SharedTransitionPolicy transitionPolicy,
        SharedEnvironmentMappingService environmentMappingService,
        SharedContractAccessService contractAccessService
    ) {
        this.entityManager = entityManager;
        this.referenceResolver = referenceResolver;
        this.participationRepository = participationRepository;
        this.applicationQueryService = applicationQueryService;
        this.sharedValidator = sharedValidator;
        this.transitionPolicy = transitionPolicy;
        this.environmentMappingService = environmentMappingService;
        this.contractAccessService = contractAccessService;
    }

    @Auditable(
        action = AuditAction.CREATE,
        event = "SHARED_PARTICIPATION_REQUESTED",
        resourceType = "SHARED_PARTICIPATION"
    )
    public SharedParticipationOutput requestParticipation(
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String contractIdentifier
    ) {
        ApplicationScopeReferenceOutput participantApplicationReference =
            applicationQueryService.findActiveScopeForUpdate(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier
            );
        SharedContract contract = contractAccessService.requiredAvailableContract(contractIdentifier);
        if (contract.getOwnerApplicationId().equals(participantApplicationReference.applicationId())) {
            throw new ValidationException(SharedMessageKeys.SCOPE_INVALID);
        }
        Optional<SharedParticipation> existingParticipation =
            participationRepository.findByContractIdAndParticipantApplicationId(
                contract.getId(),
                participantApplicationReference.applicationId()
            );
        if (existingParticipation.isPresent()) {
            if (
                !existingParticipation
                    .get()
                    .getParticipantWorkspaceId()
                    .equals(participantApplicationReference.workspaceId())
            ) {
                throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
            }
            throw new ValidationException(SharedMessageKeys.STATE_INVALID);
        }
        SharedParticipation participation = new SharedParticipation(
            contract,
            participantApplicationReference.workspaceId(),
            participantApplicationReference.applicationId(),
            LocalDateTime.now()
        );
        return referenceResolver.participationOutput(participationRepository.save(participation));
    }

    @Auditable(
        action = AuditAction.UPDATE,
        event = "SHARED_PARTICIPATION_RESUBMITTED",
        resourceType = "SHARED_PARTICIPATION"
    )
    public SharedParticipationOutput requestParticipationAgain(
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String participationIdentifier
    ) {
        ApplicationScopeReferenceOutput participantApplicationReference =
            applicationQueryService.findActiveScopeForUpdate(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier
            );
        SharedParticipation participation = findParticipantParticipation(
            participantApplicationReference,
            participationIdentifier
        );
        SharedContract contract = participation.getContract();
        contractAccessService.requireAvailableContract(contract);
        participation = requiredParticipantParticipation(participantApplicationReference, participationIdentifier);
        entityManager.refresh(participation, LockModeType.PESSIMISTIC_WRITE);
        transitionPolicy.requireTransition(participation.getStatus(), ShareStatusTypeEnum.PENDING);
        participation.requestAgain(LocalDateTime.now());
        return referenceResolver.participationOutput(participation);
    }

    @Auditable(
        action = AuditAction.UPDATE,
        event = "SHARED_PARTICIPATION_APPROVED",
        resourceType = "SHARED_PARTICIPATION"
    )
    public SharedParticipationOutput approve(
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier,
        ParticipationConfigurationInput input
    ) {
        SharedParticipation participation = requiredOwnerParticipation(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participationIdentifier
        );
        transitionPolicy.requireTransition(participation.getStatus(), ShareStatusTypeEnum.APPROVED);
        applyConfiguration(ownerWorkspaceIdentifier, participation, input);
        participation.approve(participation.getPublicationMode(), LocalDateTime.now());
        return referenceResolver.participationOutput(participation);
    }

    @Auditable(
        action = AuditAction.UPDATE,
        event = "SHARED_PARTICIPATION_CONFIGURATION_UPDATED",
        resourceType = "SHARED_PARTICIPATION"
    )
    public SharedParticipationOutput updateConfiguration(
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier,
        ParticipationConfigurationInput input
    ) {
        SharedParticipation participation = requiredOwnerParticipation(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participationIdentifier
        );
        if (!ShareStatusTypeEnum.APPROVED.name().equals(participation.getStatus().value())) {
            throw new ValidationException(SharedMessageKeys.STATE_INVALID);
        }
        applyConfiguration(ownerWorkspaceIdentifier, participation, input);
        return referenceResolver.participationOutput(participation);
    }

    private void applyConfiguration(
        String ownerWorkspaceIdentifier,
        SharedParticipation participation,
        ParticipationConfigurationInput input
    ) {
        contractAccessService.requireAvailableFeature(participation.getContract());
        sharedValidator.validateConfiguration(input);
        String participantWorkspaceIdentifier = referenceResolver.workspaceIdentifier(
            participation.getParticipantWorkspaceId()
        );
        applicationQueryService.findActiveScopeForShared(
            participation.getParticipantWorkspaceId(),
            participation.getParticipantApplicationId()
        );
        LocalDateTime configurationTimestamp = LocalDateTime.now();
        participation.configure(
            PublicationModeTypeCode.of(input.publicationModeCode()),
            environmentMappingService.build(
                ownerWorkspaceIdentifier,
                participantWorkspaceIdentifier,
                input.environmentMappings(),
                configurationTimestamp
            ),
            configurationTimestamp
        );
    }

    @Auditable(
        action = AuditAction.UPDATE,
        event = "SHARED_PARTICIPATION_REJECTED",
        resourceType = "SHARED_PARTICIPATION"
    )
    public SharedParticipationOutput reject(
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier
    ) {
        SharedParticipation participation = requiredOwnerParticipation(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participationIdentifier
        );
        transitionPolicy.requireTransition(participation.getStatus(), ShareStatusTypeEnum.REJECTED);
        participation.reject(LocalDateTime.now());
        return referenceResolver.participationOutput(participation);
    }

    @Auditable(
        action = AuditAction.DEACTIVATE,
        event = "SHARED_PARTICIPATION_REVOKED",
        resourceType = "SHARED_PARTICIPATION"
    )
    public SharedParticipationOutput revoke(
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier
    ) {
        SharedParticipation participation = requiredOwnerParticipation(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participationIdentifier
        );
        transitionPolicy.requireTransition(participation.getStatus(), ShareStatusTypeEnum.REVOKED);
        participation.revoke(LocalDateTime.now());
        return referenceResolver.participationOutput(participation);
    }

    @Auditable(
        action = AuditAction.DELETE,
        event = "SHARED_PARTICIPATION_DELETED",
        resourceType = "SHARED_PARTICIPATION"
    )
    public SharedParticipationOutput deleteParticipation(
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String participationIdentifier
    ) {
        ApplicationScopeReferenceOutput participantApplicationReference =
            applicationQueryService.findActiveScopeForUpdate(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier
            );
        SharedParticipation participation = requiredParticipantParticipation(
            participantApplicationReference,
            participationIdentifier
        );
        SharedParticipationOutput deletedParticipation = referenceResolver.participationOutput(participation);
        participationRepository.delete(participation);
        return deletedParticipation;
    }

    private SharedParticipation findParticipantParticipation(
        ApplicationScopeReferenceOutput participantApplication,
        String participationIdentifier
    ) {
        return participationRepository
            .findByIdentifierAndParticipantWorkspaceIdAndParticipantApplicationId(
                participationIdentifier,
                participantApplication.workspaceId(),
                participantApplication.applicationId()
            )
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
    }

    private SharedParticipation requiredParticipantParticipation(
        ApplicationScopeReferenceOutput participantApplication,
        String participationIdentifier
    ) {
        return participationRepository
            .findParticipantParticipationForUpdate(
                participationIdentifier,
                participantApplication.workspaceId(),
                participantApplication.applicationId()
            )
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
    }

    private SharedParticipation requiredOwnerParticipation(
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier
    ) {
        SharedContract contract = contractAccessService.requiredActiveContract(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier
        );
        return participationRepository
            .findOwnerParticipationForUpdate(contract.getId(), participationIdentifier)
            .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
    }
}
