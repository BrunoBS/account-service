package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.*;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedParticipationRepository;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.*;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedValidator;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedTransitionPolicy;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class SharedCommandService {
    private final SharedContractRepository contracts;
    private final SharedParticipationRepository participations;
    private final ApplicationQueryService applications;
    private final EnvironmentQueryService environments;
    private final SharedValidator validator;
    private final SharedTransitionPolicy transitions;

    public SharedCommandService(SharedContractRepository contracts, SharedParticipationRepository participations,
                                ApplicationQueryService applications, EnvironmentQueryService environments, SharedValidator validator,
                                SharedTransitionPolicy transitions) {
        this.contracts = contracts; this.participations = participations;
        this.applications = applications; this.environments = environments; this.validator = validator; this.transitions = transitions;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "SHARED_CONTRACT_CREATED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput createContract(String workspace, String application, SharedContractInput input) {
        activeApplication(workspace, application);
        validator.validateContract(input);
        if (contracts.existsByOwnerWorkspaceIdentifierAndOwnerApplicationIdentifierAndName(workspace, application, input.name().trim()))
            throw new ValidationException(SharedMessageKeys.DUPLICATE);
        return SharedContractOutput.from(contracts.save(new SharedContract(workspace, application,
                input.name().trim(), clean(input.description()), now())));
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SHARED_CONTRACT_UPDATED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput updateContract(String workspace, String application, String identifier, SharedContractInput input) {
        SharedContract contract = requiredActiveContract(workspace, application, identifier);
        validator.validateContract(input);
        String name = input.name().trim();
        if (!contract.getName().equals(name) && contracts.existsByOwnerWorkspaceIdentifierAndOwnerApplicationIdentifierAndNameAndIdNot(
                workspace, application, name, contract.getId())) throw new ValidationException(SharedMessageKeys.DUPLICATE);
        contract.update(name, clean(input.description()), now());
        return SharedContractOutput.from(contract);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "SHARED_CONTRACT_INACTIVATED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput inactivateContract(String workspace, String application, String identifier) {
        SharedContract contract = requiredActiveContract(workspace, application, identifier);
        contract.inactivate(now());
        return SharedContractOutput.from(contract);
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "SHARED_CONTRACT_ACTIVATED", resourceType = "SHARED_CONTRACT")
    public SharedContractOutput activateContract(String workspace, String application, String identifier) {
        activeApplication(workspace, application);
        SharedContract contract = contracts.findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(identifier, workspace, application)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        if (!LifecycleTypeCode.inactive().equals(contract.getLifecycle()))
            throw new ValidationException(SharedMessageKeys.LIFECYCLE_INVALID);
        contract.activate(now());
        return SharedContractOutput.from(contract);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "SHARED_CONTRACT_DELETED", resourceType = "SHARED_CONTRACT")
    public void deleteContract(String workspace, String application, String identifier) {
        activeApplication(workspace, application);
        SharedContract contract = requiredContract(workspace, application, identifier);
        if (!LifecycleTypeCode.inactive().equals(contract.getLifecycle()))
            throw new ValidationException(SharedMessageKeys.DELETE_REQUIRES_INACTIVE);
        contracts.delete(contract); // FK cascades participations and their mappings.
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "SHARED_PARTICIPATION_REQUESTED", resourceType = "SHARED_PARTICIPATION")
    public SharedParticipationOutput requestParticipation(String participantWorkspace, String participantApplication,
            String contractIdentifier, String ownerWorkspace, String ownerApplication) {
        activeApplication(participantWorkspace, participantApplication);
        SharedContract contract = requiredActiveContract(ownerWorkspace, ownerApplication, contractIdentifier);
        if (ownerWorkspace.equals(participantWorkspace) && ownerApplication.equals(participantApplication))
            throw new ValidationException(SharedMessageKeys.SCOPE_INVALID);
        SharedParticipation participation = participations.findByContractIdAndParticipantApplicationIdentifier(
                contract.getId(), participantApplication).orElse(null);
        if (participation == null) {
            participation = new SharedParticipation(contract, participantWorkspace, participantApplication, now());
            return SharedParticipationOutput.from(participations.save(participation));
        }
        if (!participation.getParticipantWorkspaceIdentifier().equals(participantWorkspace))
            throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
        throw new ValidationException(SharedMessageKeys.STATE_INVALID);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SHARED_PARTICIPATION_RESUBMITTED", resourceType = "SHARED_PARTICIPATION")
    public SharedParticipationOutput resubmitParticipation(String participantWorkspace, String participantApplication,
            String participationIdentifier) {
        activeApplication(participantWorkspace, participantApplication);
        SharedParticipation participation = participations.findByIdentifierAndParticipantWorkspaceIdentifierAndParticipantApplicationIdentifier(
                participationIdentifier, participantWorkspace, participantApplication)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        SharedContract contract = participation.getContract();
        if (!contract.isActive()) throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
        activeApplicationForShared(contract.getOwnerWorkspaceIdentifier(), contract.getOwnerApplicationIdentifier());
        ShareStatusTypeEnum status = ShareStatusTypeEnum.valueOf(participation.getStatus().value());
        if (status != ShareStatusTypeEnum.REJECTED && status != ShareStatusTypeEnum.REVOKED)
            throw new ValidationException(SharedMessageKeys.STATE_INVALID);
        transitions.requireTransition(participation.getStatus(), ShareStatusTypeEnum.PENDING);
        participation.resubmit(now());
        return SharedParticipationOutput.from(participation);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SHARED_PARTICIPATION_APPROVED", resourceType = "SHARED_PARTICIPATION")
    public SharedParticipationOutput approve(String ownerWorkspace, String ownerApplication, String contractIdentifier,
            String participationIdentifier, ParticipationApprovalInput input) {
        SharedContract contract = requiredActiveContract(ownerWorkspace, ownerApplication, contractIdentifier);
        SharedParticipation participation = requiredParticipation(contract, participationIdentifier);
        if (input == null || !validPublicationMode(input.publicationModeCode()))
            throw new ValidationException(SharedMessageKeys.STATE_INVALID);
        transitions.requireTransition(participation.getStatus(), ShareStatusTypeEnum.APPROVED);
        activeApplicationForShared(participation.getParticipantWorkspaceIdentifier(), participation.getParticipantApplicationIdentifier());
        EnvironmentMappingInput mappingInput = input.environmentMappings() == null
                ? new EnvironmentMappingInput(List.of()) : input.environmentMappings();
        participation.replaceMappings(buildMappings(ownerWorkspace, participation.getParticipantWorkspaceIdentifier(), mappingInput), now());
        participation.approve(PublicationModeTypeCode.of(input.publicationModeCode()), now());
        return SharedParticipationOutput.from(participation);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SHARED_PARTICIPATION_REJECTED", resourceType = "SHARED_PARTICIPATION")
    public SharedParticipationOutput reject(String ownerWorkspace, String ownerApplication, String contractIdentifier,
            String participationIdentifier) {
        SharedContract contract = requiredActiveContract(ownerWorkspace, ownerApplication, contractIdentifier);
        SharedParticipation participation = requiredParticipation(contract, participationIdentifier);
        transitions.requireTransition(participation.getStatus(), ShareStatusTypeEnum.REJECTED);
        participation.reject(now());
        return SharedParticipationOutput.from(participation);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "SHARED_PARTICIPATION_REVOKED", resourceType = "SHARED_PARTICIPATION")
    public SharedParticipationOutput revoke(String ownerWorkspace, String ownerApplication, String contractIdentifier,
            String participationIdentifier) {
        SharedContract contract = requiredActiveContract(ownerWorkspace, ownerApplication, contractIdentifier);
        SharedParticipation participation = requiredParticipation(contract, participationIdentifier);
        transitions.requireTransition(participation.getStatus(), ShareStatusTypeEnum.REVOKED);
        participation.revoke(now());
        return SharedParticipationOutput.from(participation);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SHARED_PARTICIPATION_MODE_CHANGED", resourceType = "SHARED_PARTICIPATION")
    public SharedParticipationOutput changePublicationMode(String ownerWorkspace, String ownerApplication, String contractIdentifier,
            String participationIdentifier, String modeCode) {
        SharedContract contract = requiredActiveContract(ownerWorkspace, ownerApplication, contractIdentifier);
        SharedParticipation participation = requiredParticipation(contract, participationIdentifier);
        if (!ShareStatusTypeEnum.APPROVED.name().equals(participation.getStatus().value()) || !validPublicationMode(modeCode))
            throw new ValidationException(SharedMessageKeys.STATE_INVALID);
        participation.changePublicationMode(PublicationModeTypeCode.of(modeCode), now());
        return SharedParticipationOutput.from(participation);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "SHARED_PARTICIPATION_MAPPINGS_CHANGED", resourceType = "SHARED_PARTICIPATION")
    public SharedParticipationOutput replaceMappings(String ownerWorkspace, String ownerApplication, String contractIdentifier,
            String participationIdentifier, EnvironmentMappingInput input) {
        SharedContract contract = requiredActiveContract(ownerWorkspace, ownerApplication, contractIdentifier);
        SharedParticipation participation = requiredParticipation(contract, participationIdentifier);
        if (!ShareStatusTypeEnum.APPROVED.name().equals(participation.getStatus().value()) || input == null || input.mappings() == null)
            throw new ValidationException(SharedMessageKeys.STATE_INVALID);
        activeApplicationForShared(participation.getParticipantWorkspaceIdentifier(), participation.getParticipantApplicationIdentifier());
        participation.replaceMappings(buildMappings(ownerWorkspace, participation.getParticipantWorkspaceIdentifier(), input), now());
        return SharedParticipationOutput.from(participation);
    }

    private LinkedHashSet<SharedEnvironmentMapping> buildMappings(String ownerWorkspace, String participantWorkspace,
            EnvironmentMappingInput input) {
        var replacement = new LinkedHashSet<SharedEnvironmentMapping>();
        var destinations = new LinkedHashSet<String>();
        for (EnvironmentMappingInput.Mapping mapping : input.mappings()) {
            if (mapping == null || mapping.destinationEnvironmentIdentifiers() == null)
                throw new ValidationException(SharedMessageKeys.MAPPING_INVALID);
            if (mapping.destinationEnvironmentIdentifiers().isEmpty())
                throw new ValidationException(SharedMessageKeys.MAPPING_INVALID);
            String sourceIdentifier = mapping.sourceEnvironmentIdentifier();
            validator.validateSourceIdentifier(sourceIdentifier);
            EnvironmentOutput source = environment(participantWorkspace, sourceIdentifier);
            for (String destinationIdentifier : mapping.destinationEnvironmentIdentifiers()) {
                validator.validateMappingNames(sourceIdentifier, destinationIdentifier,
                        !destinations.add(destinationIdentifier));
                EnvironmentOutput destination = environment(ownerWorkspace, destinationIdentifier);
                if (!Objects.equals(environmentBase(source), environmentBase(destination)))
                    throw new ValidationException(SharedMessageKeys.MAPPING_INVALID);
                replacement.add(new SharedEnvironmentMapping(source.identifier(), destination.identifier(), now()));
            }
        }
        return replacement;
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "SHARED_PARTICIPATION_DELETED", resourceType = "SHARED_PARTICIPATION")
    public void leave(String participantWorkspace, String participantApplication, String participationIdentifier) {
        activeApplication(participantWorkspace, participantApplication);
        SharedParticipation participation = participations.findByIdentifierAndParticipantWorkspaceIdentifierAndParticipantApplicationIdentifier(
                participationIdentifier, participantWorkspace, participantApplication)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
        participations.delete(participation);
    }

    private EnvironmentOutput environment(String workspace, String identifier) {
        try { return environments.findActiveForShared(workspace, identifier); }
        catch (NotFoundException ex) { return environments.findDefault(identifier); }
    }
    private String environmentBase(EnvironmentOutput environment) {
        return environment.workspaceIdentifier() == null || environment.parentIdentifier() == null
                ? environment.identifier() : environment.parentIdentifier();
    }
    private boolean validPublicationMode(String code) {
        if (code == null) return false;
        try { PublicationModeTypeEnum.valueOf(code); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }
    private SharedContract requiredActiveContract(String workspace, String application, String identifier) {
        activeApplicationForShared(workspace, application);
        SharedContract contract = requiredContract(workspace, application, identifier);
        if (!contract.isActive()) throw new NotFoundException(SharedMessageKeys.NOT_FOUND);
        return contract;
    }
    private SharedContract requiredContract(String workspace, String application, String identifier) {
        return contracts.findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(identifier, workspace, application)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
    }
    private SharedParticipation requiredParticipation(SharedContract contract, String identifier) {
        return participations.findByContractIdAndIdentifier(contract.getId(), identifier)
                .orElseThrow(() -> new NotFoundException(SharedMessageKeys.NOT_FOUND));
    }
    private void activeApplication(String workspace, String application) {
        applications.findByIdentifier(workspace, application);
    }
    private void activeApplicationForShared(String workspace, String application) {
        applications.findActiveForShared(workspace, application);
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private LocalDateTime now() { return LocalDateTime.now(); }
}
