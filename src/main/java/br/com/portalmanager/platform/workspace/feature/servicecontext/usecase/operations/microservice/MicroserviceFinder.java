package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;

@Component
public class MicroserviceFinder {

    private final MicroserviceRepository repository;
    private final EntityManager entityManager;

    public MicroserviceFinder(MicroserviceRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public Microservice findActive(String identifier) {
        return repository
                .findByIdentifierAndLifecycle(identifier, LifecycleTypeCode.active())
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ServiceContextMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    public Microservice findForUpdate(String identifier) {
        Long microserviceId =
                repository
                        .findInternalIdByIdentifierForUpdate(identifier)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ServiceContextMessageKeys.MICROSERVICE_NOT_FOUND));
        // Reload under the lock: an entity cached before a concurrent commit may have an old
        // version.
        entityManager.detach(entityManager.getReference(Microservice.class, microserviceId));
        return repository
                .findByIdentifierForUpdate(identifier)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ServiceContextMessageKeys.MICROSERVICE_NOT_FOUND));
    }
}
