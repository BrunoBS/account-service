package br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.participation;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.ParticipationStatusInput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation.SharedParticipationOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.validation.SharedValidator;
import org.springframework.stereotype.Service;

@Service
public class SharedParticipationStatusService {

    private final SharedParticipationCommandService participationCommandService;
    private final SharedValidator sharedValidator;

    public SharedParticipationStatusService(
        SharedParticipationCommandService participationCommandService,
        SharedValidator sharedValidator
    ) {
        this.participationCommandService = participationCommandService;
        this.sharedValidator = sharedValidator;
    }

    public SharedParticipationOutput changeStatus(
        String ownerWorkspaceIdentifier,
        String ownerApplicationIdentifier,
        String contractIdentifier,
        String participationIdentifier,
        ParticipationStatusInput input
    ) {
        return switch (sharedValidator.participationAction(input)) {
            case APPROVE -> participationCommandService.approve(
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier,
                contractIdentifier,
                participationIdentifier,
                input.approval()
            );
            case REJECT -> participationCommandService.reject(
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier,
                contractIdentifier,
                participationIdentifier
            );
            case REVOKE -> participationCommandService.revoke(
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier,
                contractIdentifier,
                participationIdentifier
            );
        };
    }
}
