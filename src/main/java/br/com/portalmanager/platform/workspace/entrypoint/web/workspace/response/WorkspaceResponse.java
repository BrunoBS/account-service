package br.com.portalmanager.platform.workspace.entrypoint.web.workspace.response;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;

import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceResponse(
        Long version,
        String identifier,
        String workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        boolean onboarding,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ApproverResponse> approvers,
        List<String> tags
) {
    public static WorkspaceResponse from(WorkspaceOutput output) {
        return new WorkspaceResponse(
                output.version(),
                output.identifier(),
                output.workspaceType(),
                output.name(),
                output.description(),
                output.requester(),
                output.acronym(),
                output.authorizerGroup(),
                output.settings(),
                output.emailGroup(),
                output.onboarding(),
                output.lifecycle(),
                output.createdAt(),
                output.updatedAt(),
                output.approvers().stream().map(ApproverResponse::from).toList(),
                output.tags()
        );
    }
}
