package br.com.portalmanager.platform.workspace.core.workspace.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import tools.jackson.databind.JsonNode;

public record WorkspaceDetails(
    WorkspaceTypeCode workspaceType,
    String name,
    String description,
    String requester,
    String acronym,
    JsonNode settings,
    String authorizerGroup,
    String emailGroup
) {}
