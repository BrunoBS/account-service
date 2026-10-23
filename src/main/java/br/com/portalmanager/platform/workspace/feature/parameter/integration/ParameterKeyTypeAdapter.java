package br.com.portalmanager.platform.workspace.feature.parameter.integration;

import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterKeyTypePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.usecase.ParameterKeyTypeService;

import org.springframework.stereotype.Component;

@Component
public class ParameterKeyTypeAdapter implements ParameterKeyTypePort {
    private final ParameterKeyTypeService catalog;

    public ParameterKeyTypeAdapter(ParameterKeyTypeService catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean existsActive(String code) {
        return catalog.existsActive(code);
    }
}
