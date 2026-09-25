package br.com.itau.portalmanager.workspace.feature.platform.usecase.feature;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;
import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@org.springframework.stereotype.Service
public class FeatureQueryService {
    private final FeatureRepository repository;
    public FeatureQueryService(FeatureRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Feature findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier).orElseThrow(() -> new IllegalArgumentException("Feature not found"));
    }
    @Transactional(readOnly = true)
    public List<Feature> findAll() { return repository.findAll(); }
    @Transactional(readOnly = true)
    public List<Feature> findByScope(String scopeCode) { return repository.findAllByScopes_Code(scopeCode); }
    @Transactional(readOnly = true)
    public java.util.Set<br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType> findScopes(String identifier) {
        return findByIdentifier(identifier).getScopes();
    }
}
