package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.repository.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.MicroserviceValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
public class MicroserviceCommandService {
    private final MicroserviceRepository repository;
    private final MicroserviceValidator validator;

    public MicroserviceCommandService(MicroserviceRepository repository) {
        this(repository, new MicroserviceValidator());
    }

    @Autowired
    public MicroserviceCommandService(MicroserviceRepository repository, MicroserviceValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public MicroserviceOutput create(CreateMicroserviceInput input) {
        validator.validateCreate(input, input != null && repository.existsByCode(input.code()),
                input != null && repository.existsByName(input.name()));
        return MicroserviceOutput.from(repository.save(new Microservice(input.code(), input.name(), input.description(), now())));
    }

    @Transactional
    public MicroserviceOutput update(String identifier, UpdateMicroserviceInput input) {
        Microservice microservice = required(identifier);
        validator.validateUpdate(input, input != null && !microservice.getName().equals(input.name())
                && repository.existsByName(input.name()));
        microservice.update(input.name(), input.description(), now());
        return MicroserviceOutput.from(microservice);
    }

    @Transactional
    public MicroserviceOutput activate(String identifier) {
        Microservice microservice = required(identifier);
        microservice.activate(now());
        return MicroserviceOutput.from(microservice);
    }

    @Transactional
    public MicroserviceOutput inactivate(String identifier) {
        Microservice microservice = required(identifier);
        microservice.inactivate(now());
        return MicroserviceOutput.from(microservice);
    }

    @Transactional
    public MicroserviceOutput delete(String identifier) {
        Microservice microservice = required(identifier);
        validator.validateDelete(microservice);
        microservice.quarantine(now());
        return MicroserviceOutput.from(microservice);
    }

    private Microservice required(String identifier) {
        return repository.findByIdentifier(identifier).orElseThrow(() -> new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
