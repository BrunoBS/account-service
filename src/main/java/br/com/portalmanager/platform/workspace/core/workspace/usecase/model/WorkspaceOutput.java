package br.com.portalmanager.platform.workspace.core.workspace.usecase.model;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record WorkspaceOutput(
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
        List<ApproverOutput> approvers,
        List<String> tags
) {

    public static WorkspaceOutput from(Workspace workspace, List<String> manualTags) {
        List<ApproverOutput> approvers = workspace.getApprovers().stream()
                .map(value -> new ApproverOutput(value.getFunctional(), value.getEmail()))
                .sorted(Comparator.comparing(ApproverOutput::functional)
                        .thenComparing(ApproverOutput::email))
                .toList();

        return new WorkspaceOutput(
                workspace.getVersion(),
                workspace.getIdentifier(),
                workspace.getWorkspaceType().value(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getRequester(),
                workspace.getAcronym(),
                workspace.getAuthorizerGroup(),
                workspace.getSettings(),
                workspace.getEmailGroup(),
                workspace.isOnboarding(),
                workspace.getLifecycle().value(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt(),
                approvers,
                manualTags == null ? List.of() : List.copyOf(manualTags)
        );
    }
}
