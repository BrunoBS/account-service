package br.com.portalmanager.platform.workspace.core.workspace.usecase.model;

import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record WorkspaceOutput(
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
    List<ApproverOutput> approvers,
    List<String> tags
) {}
