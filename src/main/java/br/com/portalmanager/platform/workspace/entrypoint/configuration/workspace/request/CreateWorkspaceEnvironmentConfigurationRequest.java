package br.com.portalmanager.platform.workspace.entrypoint.configuration.workspace.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.configuration.contract.EnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.configuration.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.configuration.workspace.usecase.model.CreateWorkspaceEnvironmentConfigurationInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

@ValidateResourceSchemaRequest(
        type = EnvironmentConfigurationContracts.TYPE,
        code = EnvironmentConfigurationContracts.WORKSPACE_CREATE)
public record CreateWorkspaceEnvironmentConfigurationRequest(
        Long version,
        String environmentIdentifier,
        JsonNode settings,
        List<PublisherInput> publishers) {
    public CreateWorkspaceEnvironmentConfigurationInput toInput() {
        return new CreateWorkspaceEnvironmentConfigurationInput(
                version, environmentIdentifier, settings, publishers);
    }
}
