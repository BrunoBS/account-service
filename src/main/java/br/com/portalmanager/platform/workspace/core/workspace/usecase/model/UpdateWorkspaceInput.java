package br.com.portalmanager.platform.workspace.core.workspace.usecase.model;

import java.util.List;

public record UpdateWorkspaceInput(
        Long version,
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
