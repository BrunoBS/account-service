package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.operations.participant;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant.ParticipantStatusInput;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.validation.ResourceSharingValidator;

import org.springframework.stereotype.Service;

@Service
public class ParticipantStatusService {

    private final ParticipantCommandService participantCommandService;
    private final ResourceSharingValidator resourceSharingValidator;

    public ParticipantStatusService(
            ParticipantCommandService participantCommandService,
            ResourceSharingValidator resourceSharingValidator) {
        this.participantCommandService = participantCommandService;
        this.resourceSharingValidator = resourceSharingValidator;
    }

    public ParticipantOutput changeStatus(
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String contractIdentifier,
            String participantIdentifier,
            ParticipantStatusInput input) {
        return switch (resourceSharingValidator.participantAction(input)) {
            case APPROVE ->
                    participantCommandService.approve(
                            ownerWorkspaceIdentifier,
                            ownerApplicationIdentifier,
                            contractIdentifier,
                            participantIdentifier,
                            input.approval());
            case REJECT ->
                    participantCommandService.reject(
                            ownerWorkspaceIdentifier,
                            ownerApplicationIdentifier,
                            contractIdentifier,
                            participantIdentifier);
            case REVOKE ->
                    participantCommandService.revoke(
                            ownerWorkspaceIdentifier,
                            ownerApplicationIdentifier,
                            contractIdentifier,
                            participantIdentifier);
        };
    }
}
