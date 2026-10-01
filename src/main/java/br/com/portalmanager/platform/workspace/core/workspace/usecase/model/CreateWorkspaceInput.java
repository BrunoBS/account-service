package br.com.portalmanager.platform.workspace.core.workspace.usecase.model;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record CreateWorkspaceInput(
        Long version,
        String workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        JsonNode settings,
        String emailGroup,
        List<ApproverInput> approvers,
        List<String> tags
) {
}
