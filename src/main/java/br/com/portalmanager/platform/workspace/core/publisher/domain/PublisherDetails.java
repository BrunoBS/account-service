package br.com.portalmanager.platform.workspace.core.publisher.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;
import tools.jackson.databind.JsonNode;

public record PublisherDetails(
    String name,
    String description,
    ResourceScopeTypeCode scope,
    boolean deprecated,
    JsonNode settings
) {}
