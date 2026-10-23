package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port;

import tools.jackson.databind.JsonNode;

import java.util.List;

public interface WorkspaceEnvironmentConfigurationPort {
    List<PublisherCopy> copyPublishers(Long workspaceId, Long environmentId);

    record PublisherCopy(String publisherCode, Integer sortOrder, JsonNode parameters) {}
}
