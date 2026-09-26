package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.service;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Service;
import br.com.portalmanager.platform.workspace.feature.platform.repository.ServiceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateServiceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.ServiceOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateServiceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.ServiceValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class ServiceCommandService {
    private final ServiceRepository repository;
    private final ServiceValidator validator;

    public ServiceCommandService(ServiceRepository repository) {
        this(repository, new ServiceValidator());
    }

    @Autowired
    public ServiceCommandService(ServiceRepository repository, ServiceValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public ServiceOutput create(CreateServiceInput input) {
        validator.validateCreate(input, input != null && repository.existsByCode(input.code()),
                input != null && repository.existsByName(input.name()));
        return ServiceOutput.from(repository.save(new Service(input.code(), input.name(), input.description(), now())));
    }

    @Transactional
    public ServiceOutput update(String identifier, UpdateServiceInput input) {
        Service service = required(identifier);
        validator.validateUpdate(input, input != null && !service.getName().equals(input.name())
                && repository.existsByName(input.name()));
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
        validator.validateDelete(service);
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
