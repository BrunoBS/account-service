package br.com.portalmanager.platform.workspace.feature.sharing.facade.contract;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractInput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractPageOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantConfigurationInput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantPageOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantStatusInput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.operations.contract.ContractCommandService;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.operations.contract.ContractQueryService;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.operations.participant.ParticipantCommandService;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.operations.participant.ParticipantQueryService;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.operations.participant.ParticipantStatusService;

import org.springframework.stereotype.Service;

@Service
public class ContractFacade {

    private final ParticipantStatusService statusService;
    private final ContractCommandService contractCommandService;
    private final ParticipantCommandService participantCommandService;
    private final ContractQueryService contractQueryService;
    private final ParticipantQueryService participantQueryService;

    public ContractFacade(
            ParticipantStatusService statusService,
            ContractCommandService contractCommandService,
            ParticipantCommandService participantCommandService,
            ContractQueryService contractQueryService,
            ParticipantQueryService participantQueryService) {
        this.statusService = statusService;
        this.contractCommandService = contractCommandService;
        this.participantCommandService = participantCommandService;
        this.contractQueryService = contractQueryService;
        this.participantQueryService = participantQueryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ContractOutput create(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            ContractInput input) {
        return contractCommandService.createContract(
                ownerWorkspaceIdentifier, ownerApplicationIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ContractPageOutput list(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String lifecycle,
            String page,
            String size) {
        return contractQueryService.listContracts(
                ownerWorkspaceIdentifier, ownerApplicationIdentifier, lifecycle, page, size);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ContractOutput find(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier) {
        return contractQueryService.findContract(
                ownerWorkspaceIdentifier, ownerApplicationIdentifier, contractIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public ContractOutput update(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            ContractInput input) {
        return contractCommandService.updateContract(
                ownerWorkspaceIdentifier, ownerApplicationIdentifier, contractIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public ContractOutput activate(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier) {
        return contractCommandService.activateContract(
                ownerWorkspaceIdentifier, ownerApplicationIdentifier, contractIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public ContractOutput inactivate(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier) {
        return contractCommandService.inactivateContract(
                ownerWorkspaceIdentifier, ownerApplicationIdentifier, contractIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier) {
        contractCommandService.deleteContract(
                ownerWorkspaceIdentifier, ownerApplicationIdentifier, contractIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ParticipantPageOutput participants(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantName,
            String participantApplicationIdentifier,
            String status,
            String page,
            String size) {
        return participantQueryService.listOwnerParticipants(
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier,
                contractIdentifier,
                participantName,
                participantApplicationIdentifier,
                status,
                page,
                size);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ParticipantOutput participant(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier) {
        return participantQueryService.findOwnerParticipantDetails(
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier,
                contractIdentifier,
                participantIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public ParticipantOutput changeParticipantStatus(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier,
            ParticipantStatusInput input) {
        return statusService.changeStatus(
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier,
                contractIdentifier,
                participantIdentifier,
                input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public ParticipantOutput updateConfiguration(
            AuthorizationContext authorizationContext,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier,
            ParticipantConfigurationInput input) {
        return participantCommandService.updateConfiguration(
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier,
                contractIdentifier,
                participantIdentifier,
                input);
    }
}
