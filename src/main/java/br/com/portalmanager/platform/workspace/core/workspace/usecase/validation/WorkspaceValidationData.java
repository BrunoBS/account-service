package br.com.portalmanager.platform.workspace.core.workspace.usecase.validation;

import java.util.List;

public record WorkspaceValidationData(
        Long version,
        String workspaceType,
        List<ApproverData> approvers
) {
}
