package br.com.portalmanager.platform.workspace.core.application.integration;

import br.com.portalmanager.platform.workspace.core.application.usecase.port.ApplicationScopeTypePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase.ApplicationScopeTypeService;

import org.springframework.stereotype.Component;

@Component
public class ApplicationScopeTypeAdapter implements ApplicationScopeTypePort {
    private final ApplicationScopeTypeService catalog;

    public ApplicationScopeTypeAdapter(ApplicationScopeTypeService catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean existsActive(String code) {
        return catalog.existsActive(code);
    }
}
