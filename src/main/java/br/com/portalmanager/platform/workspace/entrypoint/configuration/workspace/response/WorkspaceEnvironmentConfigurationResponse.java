package br.com.portalmanager.platform.workspace.entrypoint.configuration.workspace.response;

import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model.WorkspaceEnvironmentConfigurationOutput;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceEnvironmentConfigurationResponse(
        String identifier,
        String workspaceIdentifier,
        String environmentIdentifier,
        Long version,
        String lifecycle,
        JsonNode settings,
        List<PublisherOutput> publishers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static WorkspaceEnvironmentConfigurationResponse from(
            WorkspaceEnvironmentConfigurationOutput output) {
        return new WorkspaceEnvironmentConfigurationResponse(
                output.identifier(),
                output.workspaceIdentifier(),
                output.environmentIdentifier(),
                output.version(),
                output.lifecycle(),
                output.settings(),
                output.publishers(),
                output.createdAt(),
                output.updatedAt());
    }
}
