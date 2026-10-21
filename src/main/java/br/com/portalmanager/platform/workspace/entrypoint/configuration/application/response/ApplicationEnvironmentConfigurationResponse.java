package br.com.portalmanager.platform.workspace.entrypoint.configuration.application.response;

import br.com.portalmanager.platform.workspace.core.configuration.application.usecase.model.ApplicationEnvironmentConfigurationOutput;
import br.com.portalmanager.platform.workspace.core.configuration.model.PublisherOutput;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationEnvironmentConfigurationResponse(
        String identifier,
        String workspaceIdentifier,
        String applicationIdentifier,
        String environmentIdentifier,
        Long version,
        String lifecycle,
        JsonNode settings,
        List<PublisherOutput> publishers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static ApplicationEnvironmentConfigurationResponse from(
            ApplicationEnvironmentConfigurationOutput output) {
        return new ApplicationEnvironmentConfigurationResponse(
                output.identifier(),
                output.workspaceIdentifier(),
                output.applicationIdentifier(),
                output.environmentIdentifier(),
                output.version(),
                output.lifecycle(),
                output.settings(),
                output.publishers(),
                output.createdAt(),
                output.updatedAt());
    }
}
