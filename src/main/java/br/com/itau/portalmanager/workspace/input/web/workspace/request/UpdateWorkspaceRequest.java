package br.com.itau.portalmanager.workspace.input.web.workspace.request;

import br.com.itau.portalmanager.workspace.core.workspace.usecase.update.UpdateWorkspaceInput;

import java.util.List;

public record UpdateWorkspaceRequest(
        Long version,
        String workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        List<ApproverRequest> approvers,
        List<String> tags
) {
    public UpdateWorkspaceInput toInput() {
        return new UpdateWorkspaceInput(
                version,
                workspaceType,
                name,
                description,
                requester,
                acronym,
                authorizerGroup,
                settings,
                emailGroup,
                approvers == null ? null : approvers.stream()
                        .map(value -> value == null ? null : value.toInput())
                        .toList(),
                tags
        );
    }
}
