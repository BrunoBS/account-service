package br.com.itau.portalmanager.workspace.core.workspace.usecase.validation;

import java.util.List;

public record WorkspaceValidationData(
        Long version,
        String workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        String emailGroup,
        List<ApproverData> approvers
) {
}
