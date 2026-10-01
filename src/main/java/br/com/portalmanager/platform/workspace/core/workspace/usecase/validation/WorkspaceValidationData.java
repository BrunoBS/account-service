package br.com.portalmanager.platform.workspace.core.workspace.usecase.validation;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record WorkspaceValidationData(
        Long version,
        String workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        JsonNode settings,
        String emailGroup,
        List<ApproverData> approvers
) {
}
