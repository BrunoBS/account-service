package br.com.portalmanager.platform.workspace.entrypoint.configuration.application.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.configuration.application.usecase.model.UpdateApplicationEnvironmentConfigurationInput;
import br.com.portalmanager.platform.workspace.core.configuration.contract.EnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.configuration.model.PublisherInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

@ValidateResourceSchemaRequest(
        type = EnvironmentConfigurationContracts.TYPE,
        code = EnvironmentConfigurationContracts.APPLICATION_UPDATE)
public record UpdateApplicationEnvironmentConfigurationRequest(
        Long version,
        JsonNode settings,
        List<PublisherInput> publishers,
        Boolean copyWorkspacePublishers) {
    public UpdateApplicationEnvironmentConfigurationInput toInput() {
        return new UpdateApplicationEnvironmentConfigurationInput(
                version, settings, publishers, Boolean.TRUE.equals(copyWorkspacePublishers));
    }
}
