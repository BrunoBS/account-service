package br.com.portalmanager.platform.workspace.entrypoint.configuration.application.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.contract.ApplicationEnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.model.CreateApplicationEnvironmentConfigurationInput;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.usecase.model.PublisherInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

@ValidateResourceSchemaRequest(
        type = ApplicationEnvironmentConfigurationContracts.TYPE,
        code = ApplicationEnvironmentConfigurationContracts.APPLICATION_CREATE)
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
