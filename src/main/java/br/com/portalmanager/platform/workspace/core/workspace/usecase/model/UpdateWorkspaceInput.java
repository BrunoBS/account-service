package br.com.portalmanager.platform.workspace.core.workspace.usecase.model;

import java.util.List;
import tools.jackson.databind.JsonNode;

public record UpdateWorkspaceInput(
    Long version,
    String workspaceType,
    String name,
    String description,
    String requester,
    String acronym,
    String authorizerGroup,
    JsonNode settings,
    String emailGroup,
    List<ApproverInput> approvers,
    List<String> tags
) {}
