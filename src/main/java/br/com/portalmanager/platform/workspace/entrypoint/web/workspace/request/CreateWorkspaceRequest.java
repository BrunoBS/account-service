package br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record CreateWorkspaceRequest(
        String workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        JsonNode settings,
        String emailGroup,
        List<ApproverRequest> approvers,
        List<String> tags
) {
    public CreateWorkspaceInput toInput() {
        return new CreateWorkspaceInput(
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
