package br.com.portalmanager.platform.workspace.entrypoint.web.workspace.response;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public record WorkspaceResponse(
    Long version,
    String identifier,
    String workspaceType,
    String name,
    String description,
    String requester,
    String acronym,
    String authorizerGroup,
    JsonNode settings,
    String emailGroup,
    boolean onboarding,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<ApproverResponse> approvers,
    List<String> tags
) {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

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
            toJsonNode(output.settings()),
            output.emailGroup(),
            output.onboarding(),
            output.lifecycle(),
            output.createdAt(),
            output.updatedAt(),
            output.approvers().stream().map(ApproverResponse::from).toList(),
            output.tags()
        );
    }

    private static JsonNode toJsonNode(String settings) {
        if (settings == null || settings.isBlank()) {
            return null;
        }
        return JSON_MAPPER.readTree(settings);
    }
}
