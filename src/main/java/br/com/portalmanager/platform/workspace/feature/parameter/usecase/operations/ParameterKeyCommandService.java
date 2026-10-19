package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKeyMessageKeys;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.mapping.ParameterKeyMapper;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort.Reference;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParameterKeyCommandService {
    private final ParameterKeyRepository repository;
    private final EntityManager entityManager;
    private final ParameterKeyValidator validator;
    private final ParameterKeyMapper mapper;
    private final MicroserviceReferencePort microservices;
    private final ParameterKeyPublisherService publisher;
    private final ObjectProvider<ParameterKeyCommandService> self;

    public ParameterKeyCommandService(
            ParameterKeyRepository repository,
            EntityManager entityManager,
            ParameterKeyValidator validator,
            ParameterKeyMapper mapper,
            MicroserviceReferencePort microservices,
            ParameterKeyPublisherService publisher,
            ObjectProvider<ParameterKeyCommandService> self) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.validator = validator;
        this.mapper = mapper;
        this.microservices = microservices;
        this.publisher = publisher;
        this.self = self;
    }

    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "PARAMETER_KEY_CREATED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata create(CreateParameterKeyInput input) {
        validator.validateCreateInput(input);
        Reference reference =
                microservices
                        .findActiveByIdentifier(input.microserviceIdentifier())
                        .orElseThrow(
                                () ->
                                        new ValidationException(
                                                new ValidationResult(
                                                        "microserviceIdentifier",
                                                        ParameterKeyMessageKeys
                                                                .MICROSERVICE_UNAVAILABLE)));
        validator.validateUnique(reference.internalId(), input.key());
        ParameterKey resource = mapper.toDomain(input, reference.internalId(), LocalDateTime.now());
        return mapper.toMetadata(repository.saveAndFlush(resource), reference);
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARAMETER_KEY_UPDATED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata update(String identifier, UpdateParameterKeyInput input) {
        ParameterKey resource = findForUpdate(identifier);
        validator.validateUpdate(resource, input);
        resource.update(
                input.description(),
                mapper.toTypeCode(input.type()),
                input.value(),
                LocalDateTime.now());
        return mapper.toMetadata(repository.saveAndFlush(resource), reference(resource));
    }

    @Transactional
    public ParameterKeyMetadata activate(String identifier) {
        ParameterKey resource = findForUpdate(identifier);
        Reference reference = reference(resource);
        return resource.isActive()
                ? mapper.toMetadata(resource, reference)
                : self.getObject().activateTransition(resource, reference);
    }

    @Transactional
    public ParameterKeyMetadata inactivate(String identifier) {
        ParameterKey resource = findForUpdate(identifier);
        Reference reference = reference(resource);
        ParameterKeyMetadata metadata =
                !resource.isActive()
                        ? mapper.toMetadata(resource, reference)
                        : self.getObject().inactivateTransition(resource, reference);
        removeFromRuntimeAfterCommit(metadata);
        return metadata;
    }

    @Transactional
    @Auditable(
            action = AuditAction.PURGE,
            event = "PARAMETER_KEY_DELETED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata delete(String identifier) {
        ParameterKey resource = findForUpdate(identifier);
        resource.requireDeletable();
        ParameterKeyMetadata snapshot = mapper.toMetadata(resource, reference(resource));
        repository.delete(resource);
        repository.flush();
        removeFromRuntimeAfterCommit(snapshot);
        return snapshot;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(
            action = AuditAction.UPDATE,
            event = "PARAMETER_KEY_PUBLISHED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata publishIdentifier(String identifier) {
        ParameterKey resource = findForUpdate(identifier);
        validator.validatePublication(resource);
        Reference reference = reference(resource);
        resource.publish(LocalDateTime.now());
        return mapper.toMetadata(repository.saveAndFlush(resource), reference);
    }

    @Transactional
    @Auditable(
            action = AuditAction.ACTIVATE,
            event = "PARAMETER_KEY_ACTIVATED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata activateTransition(ParameterKey resource, Reference reference) {
        resource.activate(LocalDateTime.now());
        return mapper.toMetadata(repository.saveAndFlush(resource), reference);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "PARAMETER_KEY_INACTIVATED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata inactivateTransition(ParameterKey resource, Reference reference) {
        resource.inactivate(LocalDateTime.now());
        return mapper.toMetadata(repository.saveAndFlush(resource), reference);
    }

    private void removeFromRuntimeAfterCommit(ParameterKeyMetadata parameterKey) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publisher.remove(parameterKey);
                    }
                });
    }

    private ParameterKey findForUpdate(String identifier) {
        Long id =
                repository
                        .findInternalIdForUpdate(identifier)
                        .orElseThrow(
                                () -> new NotFoundException(ParameterKeyMessageKeys.NOT_FOUND));
        // Reload the locked row outside an earlier JPA/repeatable-read snapshot.
        entityManager.detach(entityManager.getReference(ParameterKey.class, id));
        return repository
                .findForUpdate(identifier)
                .orElseThrow(() -> new NotFoundException(ParameterKeyMessageKeys.NOT_FOUND));
    }

    private Reference reference(ParameterKey resource) {
        return microservices
                .findByInternalIds(List.of(resource.getMicroserviceId()))
                .get(resource.getMicroserviceId());
    }
}
