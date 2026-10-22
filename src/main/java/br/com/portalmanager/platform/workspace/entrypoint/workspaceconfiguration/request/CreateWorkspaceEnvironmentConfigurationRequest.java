package br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.contract.WorkspaceEnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.CreateWorkspaceEnvironmentConfigurationInput;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.model.PublisherInput;

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
