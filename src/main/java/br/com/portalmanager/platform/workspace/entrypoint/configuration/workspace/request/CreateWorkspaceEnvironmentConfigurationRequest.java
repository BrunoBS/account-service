package br.com.portalmanager.platform.workspace.entrypoint.configuration.workspace.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.contract.WorkspaceEnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model.CreateWorkspaceEnvironmentConfigurationInput;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model.PublisherInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

@ValidateResourceSchemaRequest(
        type = WorkspaceEnvironmentConfigurationContracts.TYPE,
        code = WorkspaceEnvironmentConfigurationContracts.WORKSPACE_CREATE)
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
