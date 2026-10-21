package br.com.portalmanager.platform.workspace.foundation.integration;

import tools.jackson.databind.JsonNode;

import java.util.List;

public interface WorkspaceEnvironmentConfigurationPort {
    List<PublisherCopy> copyPublishers(Long workspaceId, Long environmentId);

    record PublisherCopy(String publisherIdentifier, Integer sortOrder, JsonNode parameters) {}
}
