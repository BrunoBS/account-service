package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.UpdateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.microservice.MicroserviceValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MicroserviceCommandService {

    private final MicroserviceRepository repository;
    private final MicroserviceFinder finder;
    private final MicroserviceValidator validator;
    private final MicroserviceMapper mapper;

    public MicroserviceCommandService(
            MicroserviceRepository repository,
            MicroserviceFinder finder,
            MicroserviceValidator validator,
            MicroserviceMapper mapper) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "MICROSERVICE_CREATED",
            resourceType = "MICROSERVICE")
    public MicroserviceOutput create(CreateMicroserviceInput input) {
        validator.validateForCreate(input);
        Microservice microservice =
                new Microservice(input.code(), mapper.toDetails(input), LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(microservice));
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "MICROSERVICE_UPDATED",
            resourceType = "MICROSERVICE")
    public MicroserviceOutput update(String identifier, UpdateMicroserviceInput input) {
        Microservice microservice = finder.findForUpdate(identifier);
        microservice.requireActive();
        validator.validateForUpdate(microservice, input);
        microservice.update(mapper.toDetails(input), LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(microservice));
    }

    @Transactional
    @Auditable(
            action = AuditAction.ACTIVATE,
            event = "MICROSERVICE_ACTIVATED",
            resourceType = "MICROSERVICE")
    public MicroserviceOutput activate(String identifier) {
        Microservice microservice = finder.findForUpdate(identifier);
        microservice.activate(LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(microservice));
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "MICROSERVICE_DEACTIVATED",
            resourceType = "MICROSERVICE")
    public MicroserviceOutput inactivate(String identifier) {
        Microservice microservice = finder.findForUpdate(identifier);
        microservice.inactivate(LocalDateTime.now());
        return mapper.toOutput(repository.saveAndFlush(microservice));
    }
}
