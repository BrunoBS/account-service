package br.com.portalmanager.platform.workspace.feature.resourcesharing.facade.participant;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractPageOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantPageOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.contract.ContractQueryService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.participant.ParticipantCommandService;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.participant.ParticipantQueryService;

import org.springframework.stereotype.Service;

@Service
public class ParticipantFacade {

    private final ParticipantCommandService participantCommandService;
    private final ParticipantQueryService participantQueryService;
    private final ContractQueryService contractQueryService;

    public ParticipantFacade(
            ParticipantCommandService participantCommandService,
            ParticipantQueryService participantQueryService,
            ContractQueryService contractQueryService) {
        this.participantCommandService = participantCommandService;
        this.participantQueryService = participantQueryService;
        this.contractQueryService = contractQueryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.CREATE)
    public ParticipantOutput requestParticipant(
            AuthorizationContext authorizationContext,
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String contractIdentifier) {
        return participantCommandService.requestParticipant(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier,
                contractIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.UPDATE)
    public ParticipantOutput requestParticipantAgain(
            AuthorizationContext authorizationContext,
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String participantIdentifier) {
        return participantCommandService.requestParticipantAgain(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier,
                participantIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ParticipantPageOutput listParticipants(
            AuthorizationContext authorizationContext,
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String status,
            String contractIdentifier,
            String page,
            String size) {
        return participantQueryService.listParticipantParticipants(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier,
                status,
                contractIdentifier,
                page,
                size);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ContractPageOutput listContracts(
            AuthorizationContext authorizationContext,
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String page,
            String size,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier) {
        return contractQueryService.listAvailableContracts(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier,
                page,
                size,
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ContractOutput findContract(
            AuthorizationContext authorizationContext,
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String contractIdentifier) {
        return contractQueryService.findAvailableContract(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier,
                contractIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ParticipantOutput findParticipant(
            AuthorizationContext authorizationContext,
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String participantIdentifier) {
        return participantQueryService.findParticipantParticipant(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier,
                participantIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.DELETE)
    public void deleteParticipant(
            AuthorizationContext authorizationContext,
            String participantWorkspaceIdentifier,
            String participantApplicationIdentifier,
            String participantIdentifier) {
        participantCommandService.deleteParticipant(
                participantWorkspaceIdentifier,
                participantApplicationIdentifier,
                participantIdentifier);
    }
}
