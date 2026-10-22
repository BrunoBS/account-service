package br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.contract.WorkspaceEnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.UpdateWorkspaceEnvironmentConfigurationInput;

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
