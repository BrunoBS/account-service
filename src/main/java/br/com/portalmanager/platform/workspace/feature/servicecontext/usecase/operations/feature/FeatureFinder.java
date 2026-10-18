package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Component;

@Component
public class FeatureFinder {
    private final FeatureRepository features;
    private final MicroserviceRepository microservices;
    private final EntityManager entityManager;

    public FeatureFinder(
            FeatureRepository features,
            MicroserviceRepository microservices,
            EntityManager entityManager) {
        this.features = features;
        this.microservices = microservices;
        this.entityManager = entityManager;
    }

    public Feature findActive(String featureIdentifier) {
        return features.findByIdentifierAndLifecycle(featureIdentifier, LifecycleTypeCode.active())
                .orElseThrow(
                        () -> new NotFoundException(ServiceContextMessageKeys.FEATURE_NOT_FOUND));
    }

    public Feature findForUpdate(String featureIdentifier) {
        Long featureId =
                features.findInternalIdByIdentifierForUpdate(featureIdentifier)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ServiceContextMessageKeys.FEATURE_NOT_FOUND));
        // Detach before the entity query so the locking read does not compare against cached state.
        entityManager.detach(entityManager.getReference(Feature.class, featureId));
        return features.findByIdentifierForUpdate(featureIdentifier)
                .orElseThrow(
                        () -> new NotFoundException(ServiceContextMessageKeys.FEATURE_NOT_FOUND));
    }

    public Microservice findActiveMicroserviceForReference(String microserviceIdentifier) {
        Long microserviceId =
                microservices
                        .findActiveIdForReference(microserviceIdentifier)
                        .orElseThrow(
                                () ->
                                        microservices.existsByIdentifier(microserviceIdentifier)
                                                ? new ValidationException(
                                                        ServiceContextMessageKeys
                                                                .MICROSERVICE_INACTIVE)
                                                : new NotFoundException(
                                                        ServiceContextMessageKeys
                                                                .MICROSERVICE_NOT_FOUND));
        return microservices.getReferenceById(microserviceId);
    }

    /** Lock the parent first, then read the feature's current state. */
    public Feature findForUpdateWithActiveMicroservice(String featureIdentifier) {
        String microserviceIdentifier =
                features.findMicroserviceIdentifierByIdentifier(featureIdentifier)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ServiceContextMessageKeys.FEATURE_NOT_FOUND));
        Microservice microservice = findActiveMicroserviceForReference(microserviceIdentifier);
        Feature feature = findForUpdate(featureIdentifier);
        if (!feature.getMicroservice().getId().equals(microservice.getId())) {
            throw new ResourceVersionConflictException();
        }
        return feature;
    }

    public Feature findActiveForReference(String featureIdentifier) {
        Feature feature = findForUpdateWithActiveMicroservice(featureIdentifier);
        feature.requireActive();
        return feature;
    }
}
