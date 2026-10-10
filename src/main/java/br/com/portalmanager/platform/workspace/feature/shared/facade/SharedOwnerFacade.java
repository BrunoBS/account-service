package br.com.portalmanager.platform.workspace.feature.shared.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractPageOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationConfigurationInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationStatusInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationPageOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.contract.SharedContractCommandService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.contract.SharedContractQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.participation.SharedParticipationCommandService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.participation.SharedParticipationQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.participation.SharedParticipationStatusService;
import org.springframework.stereotype.Service;

@Service
public class SharedOwnerFacade {

    private final SharedParticipationStatusService statusService;
    private final SharedContractCommandService contractCommandService;
    private final SharedParticipationCommandService participationCommandService;
    private final SharedContractQueryService contractQueryService;
    private final SharedParticipationQueryService participationQueryService;

    public SharedOwnerFacade(
        SharedParticipationStatusService statusService,
        SharedContractCommandService contractCommandService,
        SharedParticipationCommandService participationCommandService,
        SharedContractQueryService contractQueryService,
        SharedParticipationQueryService participationQueryService
    ) {
        this.statusService = statusService;
        this.contractCommandService = contractCommandService;
        this.participationCommandService = participationCommandService;
        this.contractQueryService = contractQueryService;
        this.participationQueryService = participationQueryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public SharedContractOutput create(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        SharedContractInput input
    ) {
        return contractCommandService.createContract(ownerWorkspaceIdentifier, ownerApplicationIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedContractPageOutput list(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String page,
        String size
    ) {
        return contractQueryService.listContracts(ownerWorkspaceIdentifier, ownerApplicationIdentifier, page, size);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedContractOutput find(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier
    ) {
        return contractQueryService.findContract(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public SharedContractOutput update(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        SharedContractInput input
    ) {
        return contractCommandService.updateContract(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            input
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public SharedContractOutput activate(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier
    ) {
        return contractCommandService.activateContract(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public SharedContractOutput inactivate(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier
    ) {
        return contractCommandService.inactivateContract(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier
    ) {
        contractCommandService.deleteContract(ownerWorkspaceIdentifier, ownerApplicationIdentifier, contractIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedParticipationPageOutput participants(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participantName,
        String participantApplicationIdentifier,
        String status,
        String page,
        String size
    ) {
        return participationQueryService.listOwnerParticipations(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participantName,
            participantApplicationIdentifier,
            status,
            page,
            size
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedParticipationOutput participation(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier
    ) {
        return participationQueryService.findOwnerParticipationDetails(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participationIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public SharedParticipationOutput changeParticipationStatus(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier,
        ParticipationStatusInput input
    ) {
        return statusService.changeStatus(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participationIdentifier,
            input
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public SharedParticipationOutput updateConfiguration(
        AuthorizationContext authorizationContext,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier,
        ParticipationConfigurationInput input
    ) {
        return participationCommandService.updateConfiguration(
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier,
            contractIdentifier,
            participationIdentifier,
            input
        );
    }
}
