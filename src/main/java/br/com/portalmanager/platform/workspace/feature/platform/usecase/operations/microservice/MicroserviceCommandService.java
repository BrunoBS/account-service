package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;

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

    @Auditable(action = AuditAction.CREATE, event = "MICROSERVICE_CREATED", resourceType = "MICROSERVICE")
    @Transactional
    public MicroserviceOutput create(CreateMicroserviceInput input) {
        validator.validateCreate(input, input != null && repository.existsByCode(input.code()),
                input != null && repository.existsByName(input.name()));
        validator.validateSettings(input.code(), input.settings());
        return MicroserviceOutput.from(repository.save(new Microservice(
                input.code(), input.name(), input.description(), input.settings(), now())));
    }

    @Auditable(action = AuditAction.UPDATE, event = "MICROSERVICE_UPDATED", resourceType = "MICROSERVICE")
    @Transactional
    public MicroserviceOutput update(String identifier, UpdateMicroserviceInput input) {
        Microservice microservice = required(identifier);
        validator.validateUpdate(input, input != null && !microservice.getName().equals(input.name())
                && repository.existsByName(input.name()));
        validator.validateSettings(microservice.getCode(), input.settings());
        microservice.update(input.name(), input.description(), input.settings(), now());
        return MicroserviceOutput.from(microservice);
    }

    @Auditable(action = AuditAction.ACTIVATE, event = "MICROSERVICE_ACTIVATED", resourceType = "MICROSERVICE")
    @Transactional public MicroserviceOutput activate(String identifier) { Microservice m = required(identifier); m.activate(now()); return MicroserviceOutput.from(m); }
    @Auditable(action = AuditAction.DEACTIVATE, event = "MICROSERVICE_DEACTIVATED", resourceType = "MICROSERVICE")
    @Transactional public MicroserviceOutput inactivate(String identifier) { Microservice m = required(identifier); m.inactivate(now()); return MicroserviceOutput.from(m); }
    @Auditable(action = AuditAction.DELETE, event = "MICROSERVICE_DELETED", resourceType = "MICROSERVICE")
    @Transactional public MicroserviceOutput delete(String identifier) { Microservice m = required(identifier); validator.validateDelete(m); m.quarantine(now()); return MicroserviceOutput.from(m); }
    private Microservice required(String identifier) { return repository.findByIdentifier(identifier).orElseThrow(() -> new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND)); }
    private LocalDateTime now() { return LocalDateTime.now(); }
}
