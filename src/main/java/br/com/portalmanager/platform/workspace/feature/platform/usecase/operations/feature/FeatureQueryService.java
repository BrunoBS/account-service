package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureReferenceOutput;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class FeatureQueryService {

    private final FeatureRepository repository;
    private final FeatureContextRelationRepository relations;

    public FeatureQueryService(FeatureRepository repository, FeatureContextRelationRepository relations) {
        this.repository = repository;
        this.relations = relations;
    }

    /** Locks the feature until the enclosing contract transaction commits. */
    @Transactional
    public Long findShareableInternalId(String identifier) {
        Feature feature = repository
            .findByIdentifierForUpdate(identifier)
            .filter(reference -> reference.isActive() && reference.getMicroservice().isActive())
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
        if (!feature.isShareable()) throw new ValidationException(PlatformMessageKeys.FEATURE_NOT_SHAREABLE);
        return feature.getId();
    }

    @Transactional(readOnly = true)
    public FeatureReferenceOutput findReferenceByInternalId(Long featureId) {
        Feature feature = repository
            .findById(featureId)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
        return new FeatureReferenceOutput(feature.getIdentifier(), feature.getName());
    }

    @Transactional(readOnly = true)
    public Map<Long, FeatureReferenceOutput> findReferencesByInternalIds(Collection<Long> featureIds) {
        if (featureIds.isEmpty()) return Map.of();
        return repository
            .findByIdIn(featureIds)
            .stream()
            .collect(
                Collectors.toMap(Feature::getId, feature ->
                    new FeatureReferenceOutput(feature.getIdentifier(), feature.getName())
                )
            );
    }

    @Transactional(readOnly = true)
    public Map<Long, FeatureReferenceOutput> findAvailableReferencesForShared(Collection<Long> featureIds) {
        if (featureIds.isEmpty()) return Map.of();
        return repository
            .findByIdIn(featureIds)
            .stream()
            .filter(feature -> feature.isActive() && feature.isShareable() && feature.getMicroservice().isActive())
            .collect(
                Collectors.toMap(Feature::getId, feature ->
                    new FeatureReferenceOutput(feature.getIdentifier(), feature.getName())
                )
            );
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long featureId) {
        return repository
            .findIdentifierById(featureId)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findAvailableIdentifiersForShared(Collection<Long> featureIds) {
        return findAvailableReferencesForShared(featureIds)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().identifier()));
    }

    @Transactional(readOnly = true)
    public FeatureOutput findByIdentifier(String identifier) {
        return repository
            .findByIdentifier(identifier)
            .map(FeatureOutput::from)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<FeatureOutput> findAll() {
        return repository.findAll().stream().map(FeatureOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureOutput> findByContext(String contextCode) {
        return relations.findFeaturesByContextCode(contextCode).stream().map(FeatureOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureContextOutput> findContexts(String identifier) {
        repository
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
        return relations.findContextsByFeatureIdentifier(identifier).stream().map(FeatureContextOutput::from).toList();
    }
}
