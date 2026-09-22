package br.com.itau.portalmanager.workspace.core.workspace.usecase.model;

import java.util.List;

public record CreateWorkspaceInput(
        String workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        List<ApproverInput> approvers,
        List<String> tags
) {
}
