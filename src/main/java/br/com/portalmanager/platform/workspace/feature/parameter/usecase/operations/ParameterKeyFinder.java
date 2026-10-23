package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.*;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;

@Component
public class ParameterKeyFinder {
    private final ParameterKeyRepository repository;
    private final EntityManager entityManager;

    public ParameterKeyFinder(ParameterKeyRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public ParameterKey findForUpdate(String identifier) {
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
}
