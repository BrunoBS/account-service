package br.com.portalmanager.platform.workspace.core.application.integration;

import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.foundation.integration.ApplicationReferencePort;

import org.springframework.stereotype.Component;

@Component
public class ApplicationReferenceAdapter implements ApplicationReferencePort {
    private final ApplicationQueryService applications;

    public ApplicationReferenceAdapter(ApplicationQueryService applications) {
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
