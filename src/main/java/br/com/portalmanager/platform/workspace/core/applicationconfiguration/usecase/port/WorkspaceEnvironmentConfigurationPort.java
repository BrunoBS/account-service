package br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port;

import tools.jackson.databind.JsonNode;

import java.util.List;

public interface WorkspaceEnvironmentConfigurationPort {
    void requireActive(Long workspaceId, Long environmentId);

    List<PublisherCopy> copyPublishers(Long workspaceId, Long environmentId);

    record PublisherCopy(String publisherCode, Integer sortOrder, JsonNode parameters) {}
}
