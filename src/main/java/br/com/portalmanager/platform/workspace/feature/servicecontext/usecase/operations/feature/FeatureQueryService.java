package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature;

import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.feature.FeatureMapper;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository.FeatureReference;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.feature.FeatureValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeatureQueryService {
    private final FeatureRepository repository;
    private final FeatureGroupRepository groups;
    private final FeatureFinder finder;
    private final FeatureMapper mapper;
    private final FeatureValidator validator;

    public FeatureQueryService(
            FeatureRepository repository,
            FeatureGroupRepository groups,
            FeatureFinder finder,
            FeatureMapper mapper,
            FeatureValidator validator) {
        this.repository = repository;
        this.groups = groups;
        this.finder = finder;
        this.mapper = mapper;
        this.validator = validator;
    }

    /** Locks the parent and feature until the enclosing contract transaction commits. */
    @Transactional
    public Long findShareableInternalIdByIdentifier(String featureIdentifier) {
        Feature feature;
        try {
            feature = finder.findActiveForReference(featureIdentifier);
        } catch (ValidationException exception) {
            // An unavailable parent makes the feature unavailable to the shared integration.
            throw new NotFoundException(ServiceContextMessageKeys.FEATURE_NOT_FOUND);
        }
        if (!feature.isShareable())
            throw new ValidationException(ServiceContextMessageKeys.FEATURE_NOT_SHAREABLE);
        return feature.getId();
    }

    @Transactional(readOnly = true)
    public FeatureReferenceOutput findReferenceByInternalId(Long featureId) {
        FeatureReferenceOutput reference =
                findReferencesByInternalIds(List.of(featureId)).get(featureId);
        if (reference == null)
            throw new NotFoundException(ServiceContextMessageKeys.FEATURE_NOT_FOUND);
        return reference;
    }

    @Transactional(readOnly = true)
    public Map<Long, FeatureReferenceOutput> findReferencesByInternalIds(
            Collection<Long> featureIds) {
        if (featureIds.isEmpty()) return Map.of();
        return mapper.toReferences(repository.findReferences(featureIds));
    }

    @Transactional(readOnly = true)
    public Map<Long, FeatureReferenceOutput> findAvailableReferencesForResourceSharing(
            Collection<Long> featureIds) {
        if (featureIds.isEmpty()) return Map.of();
        return mapper.toReferences(repository.findAvailableReferencesForResourceSharing(featureIds));
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long featureId) {
        return repository
                .findIdentifierById(featureId)
                .orElseThrow(
                        () -> new NotFoundException(ServiceContextMessageKeys.FEATURE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findAvailableIdentifiersForResourceSharing(
            Collection<Long> featureIds) {
        return findAvailableReferencesForResourceSharing(featureIds).entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey, entry -> entry.getValue().identifier()));
    }

    @Transactional(readOnly = true)
    public FeatureOutput findByIdentifier(String featureIdentifier) {
        return mapper.toOutput(finder.findActive(featureIdentifier));
    }

    @Transactional(readOnly = true)
    public Page<FeatureOutput> findAll(
            String featureGroupIdentifier, String lifecycle, Pageable pageable) {
        validator.validateQuery(pageable);
        String normalizedGroupIdentifier =
                featureGroupIdentifier == null || featureGroupIdentifier.isBlank()
                        ? null
                        : featureGroupIdentifier.trim();
        if (normalizedGroupIdentifier != null
                && !groups.existsByIdentifier(normalizedGroupIdentifier)) {
            throw new NotFoundException(ServiceContextMessageKeys.FEATURE_GROUP_NOT_FOUND);
        }
        return repository
                .findAll(
                        normalizedGroupIdentifier,
                        LifecycleFilter.resolve(lifecycle).value(),
                        pageable)
                .map(mapper::toOutput);
    }

}
