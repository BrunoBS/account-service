package br.com.portalmanager.platform.workspace.feature.shared.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractPageOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationPageOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.contract.SharedContractQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.participation.SharedParticipationCommandService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.participation.SharedParticipationQueryService;
import org.springframework.stereotype.Service;

@Service
public class SharedParticipantFacade {

    private final SharedParticipationCommandService participationCommandService;
    private final SharedParticipationQueryService participationQueryService;
    private final SharedContractQueryService contractQueryService;

    public SharedParticipantFacade(
        SharedParticipationCommandService participationCommandService,
        SharedParticipationQueryService participationQueryService,
        SharedContractQueryService contractQueryService
    ) {
        this.participationCommandService = participationCommandService;
        this.participationQueryService = participationQueryService;
        this.contractQueryService = contractQueryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.CREATE)
    public SharedParticipationOutput requestParticipation(
        AuthorizationContext authorizationContext,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String contractIdentifier
    ) {
        return participationCommandService.requestParticipation(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier,
            contractIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.UPDATE)
    public SharedParticipationOutput requestParticipationAgain(
        AuthorizationContext authorizationContext,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String participationIdentifier
    ) {
        return participationCommandService.requestParticipationAgain(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier,
            participationIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedParticipationPageOutput listParticipations(
        AuthorizationContext authorizationContext,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String status,
        String contractIdentifier,
        String page,
        String size
    ) {
        return participationQueryService.listParticipantParticipations(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier,
            status,
            contractIdentifier,
            page,
            size
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedContractPageOutput listContracts(
        AuthorizationContext authorizationContext,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String page,
        String size,
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier
    ) {
        return contractQueryService.listAvailableContracts(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier,
            page,
            size,
            ownerWorkspaceIdentifier,
            ownerApplicationIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedContractOutput findContract(
        AuthorizationContext authorizationContext,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String contractIdentifier
    ) {
        return contractQueryService.findAvailableContract(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier,
            contractIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedParticipationOutput findParticipation(
        AuthorizationContext authorizationContext,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String participationIdentifier
    ) {
        return participationQueryService.findParticipantParticipation(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier,
            participationIdentifier
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.DELETE)
    public void deleteParticipation(
        AuthorizationContext authorizationContext,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String participationIdentifier
    ) {
        participationCommandService.deleteParticipation(
            participantWorkspaceIdentifier,
            participantApplicationIdentifier,
            participationIdentifier
        );
    }
}
