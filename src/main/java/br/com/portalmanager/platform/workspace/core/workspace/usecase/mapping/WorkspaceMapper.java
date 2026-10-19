package br.com.portalmanager.platform.workspace.core.workspace.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceDetails;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceOnboardingReference;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.ApproverOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOnboardingReferenceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Comparator;
import tools.jackson.databind.JsonNode;

@Component
public class WorkspaceMapper {

    public WorkspaceOnboardingReferenceOutput toOnboardingReference(WorkspaceOnboardingReference reference) {
        return new WorkspaceOnboardingReferenceOutput(reference.getId(), reference.getWorkspaceType(), reference.getOnboarding());
    }

    public WorkspaceOutput toOutput(Workspace workspace, List<String> manualTags) {
        List<ApproverOutput> approvers = workspace.getApprovers().stream()
            .map(approver -> new ApproverOutput(approver.getFunctional(), approver.getEmail()))
            .sorted(Comparator.comparing(ApproverOutput::functional).thenComparing(ApproverOutput::email))
            .toList();
        return new WorkspaceOutput(
            workspace.getVersion(), workspace.getIdentifier(), workspace.getWorkspaceType().value(),
            workspace.getName(), workspace.getDescription(), workspace.getRequester(), workspace.getAcronym(),
            workspace.getAuthorizerGroup(), workspace.getSettings(), workspace.getEmailGroup(), workspace.isOnboarding(),
            workspace.getLifecycle().value(), workspace.getCreatedAt(), workspace.getUpdatedAt(), approvers,
            manualTags == null ? List.of() : List.copyOf(manualTags)
        );
    }

    public WorkspaceDetails toDetails(CreateWorkspaceInput input) {
        return toDetails(
            input.workspaceType(),
            input.name(),
            input.description(),
            input.requester(),
            input.acronym(),
            input.settings(),
            input.authorizerGroup(),
            input.emailGroup()
        );
    }

    public WorkspaceDetails toDetails(UpdateWorkspaceInput input) {
        return toDetails(
            input.workspaceType(),
            input.name(),
            input.description(),
            input.requester(),
            input.acronym(),
            input.settings(),
            input.authorizerGroup(),
            input.emailGroup()
        );
    }

    private WorkspaceDetails toDetails(
        String workspaceType,
        String name,
        String description,
        String requester,
        String acronym,
        JsonNode settings,
        String authorizerGroup,
        String emailGroup
    ) {
        return new WorkspaceDetails(
            WorkspaceTypeCode.of(workspaceType),
            name,
            description,
            requester,
            acronym,
            settings,
            authorizerGroup,
            emailGroup
        );
    }
}
