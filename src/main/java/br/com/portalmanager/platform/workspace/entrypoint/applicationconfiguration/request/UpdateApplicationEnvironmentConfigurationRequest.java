package br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.contract.ApplicationEnvironmentConfigurationContracts;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.PublisherInput;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.UpdateApplicationEnvironmentConfigurationInput;

import tools.jackson.databind.JsonNode;

import java.util.List;

@ValidateResourceSchemaRequest(
        type = ApplicationEnvironmentConfigurationContracts.TYPE,
        code = ApplicationEnvironmentConfigurationContracts.APPLICATION_UPDATE)
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
