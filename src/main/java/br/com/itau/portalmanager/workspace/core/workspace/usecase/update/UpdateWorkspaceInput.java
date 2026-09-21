package br.com.itau.portalmanager.workspace.core.workspace.usecase.update;

import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.ApproverInput;

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
