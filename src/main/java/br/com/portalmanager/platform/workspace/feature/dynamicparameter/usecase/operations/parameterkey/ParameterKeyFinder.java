package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.domain.parameterkey.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.domain.parameterkey.ParameterKeyMessageKeys;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;

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
        // Reload the current locked row, even after an earlier JPA/repeatable-read snapshot.
        entityManager.detach(entityManager.getReference(ParameterKey.class, id));
        return repository
                .findForUpdate(identifier)
                .orElseThrow(() -> new NotFoundException(ParameterKeyMessageKeys.NOT_FOUND));
    }
}
