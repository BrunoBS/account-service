package br.com.portalmanager.platform.workspace.entrypoint.configuration.workspace.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.contract.WorkspaceEnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model.UpdateWorkspaceEnvironmentConfigurationInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

@ValidateResourceSchemaRequest(
        type = WorkspaceEnvironmentConfigurationContracts.TYPE,
        code = WorkspaceEnvironmentConfigurationContracts.WORKSPACE_UPDATE)
public record UpdateWorkspaceEnvironmentConfigurationRequest(
        Long version, JsonNode settings, List<PublisherInput> publishers) {
    public UpdateWorkspaceEnvironmentConfigurationInput toInput() {
        return new UpdateWorkspaceEnvironmentConfigurationInput(version, settings, publishers);
    }
}
