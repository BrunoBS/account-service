package br.com.portalmanager.platform.workspace.entrypoint.configuration.workspace.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.configuration.contract.EnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.configuration.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.usecase.model.UpdateWorkspaceEnvironmentConfigurationInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

@ValidateResourceSchemaRequest(
        type = EnvironmentConfigurationContracts.TYPE,
        code = EnvironmentConfigurationContracts.WORKSPACE_UPDATE)
public record UpdateWorkspaceEnvironmentConfigurationRequest(
        Long version, JsonNode settings, List<PublisherInput> publishers) {
    public UpdateWorkspaceEnvironmentConfigurationInput toInput() {
        return new UpdateWorkspaceEnvironmentConfigurationInput(version, settings, publishers);
    }
}
