package br.com.portalmanager.platform.workspace.core.workspace.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceDetails;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class WorkspaceMapper {

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
