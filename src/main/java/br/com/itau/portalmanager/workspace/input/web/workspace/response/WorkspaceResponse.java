package br.com.itau.portalmanager.workspace.input.web.workspace.response;

import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.WorkspaceOutput;

import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceResponse(
        Long id,
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
                output.id(),
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
                output.lifecycle().name(),
                output.createdAt(),
                output.updatedAt(),
                output.approvers().stream().map(ApproverResponse::from).toList(),
                output.tags()
        );
    }
}
