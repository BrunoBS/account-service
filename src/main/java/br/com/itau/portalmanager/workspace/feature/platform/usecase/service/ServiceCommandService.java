package br.com.itau.portalmanager.workspace.feature.platform.usecase.service;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Service;
import br.com.itau.portalmanager.workspace.feature.platform.repository.ServiceRepository;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.CreateServiceInput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.ServiceOutput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.UpdateServiceInput;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class ServiceCommandService {
    private final ServiceRepository repository;

    public ServiceCommandService(ServiceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ServiceOutput create(CreateServiceInput input) {
        if (repository.existsByCode(input.code())) throw new IllegalArgumentException("Service code already exists");
        if (repository.existsByName(input.name())) throw new IllegalArgumentException("Service name already exists");
        return ServiceOutput.from(repository.save(new Service(input.code(), input.name(), input.description(), now())));
    }

    @Transactional
    public ServiceOutput update(String identifier, UpdateServiceInput input) {
        Service service = required(identifier);
        if (!service.getName().equals(input.name()) && repository.existsByName(input.name())) {
            throw new IllegalArgumentException("Service name already exists");
        }
        service.update(input.name(), input.description(), now());
        return ServiceOutput.from(service);
    }

    @Transactional
    public ServiceOutput activate(String identifier) {
        Service service = required(identifier);
        service.activate(now());
        return ServiceOutput.from(service);
    }

    @Transactional
    public ServiceOutput inactivate(String identifier) {
        Service service = required(identifier);
        service.inactivate(now());
        return ServiceOutput.from(service);
    }

    @Transactional
    public ServiceOutput delete(String identifier) {
        Service service = required(identifier);
        if (!service.getFeatures().isEmpty()) throw new IllegalStateException("Service with features cannot be quarantined");
        service.quarantine(now());
        return ServiceOutput.from(service);
    }

    private Service required(String identifier) {
        return repository.findByIdentifier(identifier).orElseThrow(() -> new IllegalArgumentException("Service not found"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
