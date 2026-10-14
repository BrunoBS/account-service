package br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceNormalizer;
import java.util.List;
import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(type = "WORKSPACE", code = "workspace", normalizer = WorkspaceNormalizer.class)
public record UpdateWorkspaceRequest(
    Long version,
    String workspaceType,
    String name,
    String description,
    String requester,
    String acronym,
    String authorizerGroup,
    JsonNode settings,
    String emailGroup,
    List<ApproverRequest> approvers,
    List<String> tags
) {
    public UpdateWorkspaceInput toInput() {
        return new UpdateWorkspaceInput(
            version,
            workspaceType,
            name,
            description,
            requester,
            acronym,
            authorizerGroup,
            settings,
            emailGroup,
            approvers == null ? null : approvers.stream().map(ApproverRequest::toInput).toList(),
            tags
        );
    }
}
