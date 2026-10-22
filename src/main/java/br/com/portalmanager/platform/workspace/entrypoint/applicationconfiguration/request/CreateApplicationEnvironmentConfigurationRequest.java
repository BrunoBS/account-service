package br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.CreateApplicationEnvironmentConfigurationInput;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

@ValidateResourceSchemaRequest(type = "PLATFORM", code = "application-environment-configuration")
public record CreateApplicationEnvironmentConfigurationRequest(
        Long version,
        String environmentIdentifier,
        JsonNode settings,
        List<PublisherInput> publishers,
        Boolean copyWorkspacePublishers) {
    public CreateApplicationEnvironmentConfigurationInput toInput() {
        return new CreateApplicationEnvironmentConfigurationInput(
                version,
                environmentIdentifier,
                settings,
                publishers,
                Boolean.TRUE.equals(copyWorkspacePublishers));
    }
}
