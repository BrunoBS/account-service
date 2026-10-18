package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;

@Component
public class FeatureGroupFinder {
    private final FeatureGroupRepository repository;
    private final EntityManager entityManager;

    public FeatureGroupFinder(FeatureGroupRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public FeatureGroup findActive(String featureGroupIdentifier) {
        return repository
                .findByIdentifierAndLifecycle(featureGroupIdentifier, LifecycleTypeCode.active())
                .orElseThrow(
                        () -> new NotFoundException(ServiceContextMessageKeys.FEATURE_GROUP_NOT_FOUND));
    }

    public FeatureGroup findForUpdate(String featureGroupIdentifier) {
        Long featureGroupId =
                repository
                        .findInternalIdByIdentifierForUpdate(featureGroupIdentifier)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ServiceContextMessageKeys.FEATURE_GROUP_NOT_FOUND));
        // Reload after the scalar locking read, including entities already cached by JPA.
        entityManager.detach(entityManager.getReference(FeatureGroup.class, featureGroupId));
        return repository
                .findByIdentifierForUpdate(featureGroupIdentifier)
                .orElseThrow(
                        () -> new NotFoundException(ServiceContextMessageKeys.FEATURE_GROUP_NOT_FOUND));
    }
}
