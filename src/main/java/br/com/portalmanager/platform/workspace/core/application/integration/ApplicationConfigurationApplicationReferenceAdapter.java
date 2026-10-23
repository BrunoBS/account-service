package br.com.portalmanager.platform.workspace.core.application.integration;

import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.ApplicationReferencePort;

import org.springframework.stereotype.Component;

@Component
public class ApplicationConfigurationApplicationReferenceAdapter
        implements ApplicationReferencePort {
    private final ApplicationQueryService applications;

    public ApplicationConfigurationApplicationReferenceAdapter(
            ApplicationQueryService applications) {
        this.applications = applications;
    }

    @Override
    public Long resolveActive(String workspace, String application) {
        return applications.findActiveScope(workspace, application).applicationId();
    }

    @Override
    public Long resolveActiveForUpdate(String workspace, String application) {
        return applications.findActiveScopeForUpdate(workspace, application).applicationId();
    }
}
