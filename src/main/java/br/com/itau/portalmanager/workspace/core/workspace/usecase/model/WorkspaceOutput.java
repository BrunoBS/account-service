package br.com.itau.portalmanager.workspace.core.workspace.usecase.model;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycle.domain.LifecycleTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.workspace.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.authorization.resource.AuthorizableResource;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record WorkspaceOutput(
        Long id,
        Long version,
        String identifier,
        WorkspaceTypeEnum workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        boolean onboarding,
        LifecycleTypeEnum lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ApproverOutput> approvers,
        List<String> tags
) implements AuthorizableResource {

    public static WorkspaceOutput from(Workspace workspace, List<String> manualTags) {
        List<ApproverOutput> approvers = workspace.getApprovers().stream()
                .map(value -> new ApproverOutput(value.getFunctional(), value.getEmail()))
                .sorted(Comparator.comparing(ApproverOutput::functional)
                        .thenComparing(ApproverOutput::email))
                .toList();

        return new WorkspaceOutput(
                workspace.getId(),
                workspace.getVersion(),
                workspace.getIdentifier(),
                workspace.getWorkspaceType(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getRequester(),
                workspace.getAcronym(),
                workspace.getAuthorizerGroup(),
                workspace.getSettings(),
                workspace.getEmailGroup(),
                workspace.isOnboarding(),
                workspace.getLifecycle(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt(),
                approvers,
                manualTags == null ? List.of() : List.copyOf(manualTags)
        );
    }

    @Override
    public String getAuthorizerGroup() {
        return authorizerGroup;
    }
}
