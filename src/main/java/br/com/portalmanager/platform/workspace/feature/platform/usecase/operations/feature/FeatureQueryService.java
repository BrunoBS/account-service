package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.repository.context.ContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.feature.FeatureRepository.FeatureReference;
import br.com.portalmanager.platform.workspace.feature.platform.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.feature.FeatureValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureQueryService {
    private final FeatureRepository repository;
    private final ContextRepository contexts;
    private final FeatureFinder finder;
    private final FeatureMapper mapper;
    private final FeatureValidator validator;

    public FeatureQueryService(FeatureRepository repository, ContextRepository contexts,
        FeatureFinder finder, FeatureMapper mapper, FeatureValidator validator) {
        this.repository = repository;
        this.contexts = contexts;
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
            throw new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND);
        }
        if (!feature.isShareable()) throw new ValidationException(PlatformMessageKeys.FEATURE_NOT_SHAREABLE);
        return feature.getId();
    }

    @Transactional(readOnly = true)
    public FeatureReferenceOutput findReferenceByInternalId(Long featureId) {
        FeatureReferenceOutput reference = findReferencesByInternalIds(List.of(featureId)).get(featureId);
        if (reference == null) throw new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND);
        return reference;
    }

    @Transactional(readOnly = true)
    public Map<Long, FeatureReferenceOutput> findReferencesByInternalIds(Collection<Long> featureIds) {
        if (featureIds.isEmpty()) return Map.of();
        return toReferences(repository.findReferences(featureIds));
    }

    @Transactional(readOnly = true)
    public Map<Long, FeatureReferenceOutput> findAvailableReferencesForShared(Collection<Long> featureIds) {
        if (featureIds.isEmpty()) return Map.of();
        return toReferences(repository.findAvailableReferencesForShared(featureIds));
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long featureId) {
        return repository.findIdentifierById(featureId)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findAvailableIdentifiersForShared(Collection<Long> featureIds) {
        return findAvailableReferencesForShared(featureIds).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().identifier()));
    }

    @Transactional(readOnly = true)
    public FeatureOutput findByIdentifier(String featureIdentifier) {
        return mapper.toOutput(finder.findActive(featureIdentifier));
    }

    @Transactional(readOnly = true)
    public Page<FeatureOutput> findAll(String contextIdentifier, String lifecycle, Pageable pageable) {
        validator.validateQuery(pageable);
        String normalizedContextIdentifier = contextIdentifier == null || contextIdentifier.isBlank()
            ? null : contextIdentifier.trim();
        if (normalizedContextIdentifier != null && !contexts.existsByIdentifier(normalizedContextIdentifier)) {
            throw new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND);
        }
        return repository.findAll(normalizedContextIdentifier, LifecycleFilter.resolve(lifecycle).value(), pageable)
            .map(mapper::toOutput);
    }

    private Map<Long, FeatureReferenceOutput> toReferences(List<FeatureReference> references) {
        return references.stream().collect(Collectors.toMap(FeatureReference::getId,
            reference -> new FeatureReferenceOutput(reference.getIdentifier(), reference.getName())));
    }
}
