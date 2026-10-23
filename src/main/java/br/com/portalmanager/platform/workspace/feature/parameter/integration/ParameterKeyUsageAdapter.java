package br.com.portalmanager.platform.workspace.feature.parameter.integration;

import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.port.MicroserviceUsagePort;

import org.springframework.stereotype.Component;

@Component
public class ParameterKeyUsageAdapter implements MicroserviceUsagePort {
    private final ParameterKeyRepository repository;

    public ParameterKeyUsageAdapter(ParameterKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isInUse(Long microserviceId) {
        return repository.existsByMicroserviceId(microserviceId);
    }
}
