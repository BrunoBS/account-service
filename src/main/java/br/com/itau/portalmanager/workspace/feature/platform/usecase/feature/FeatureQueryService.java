package br.com.itau.portalmanager.workspace.feature.platform.usecase.feature;

import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureRepository;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.FeatureOutput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.FeatureScopeOutput;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
public class FeatureQueryService {
    private final FeatureRepository repository;

    public FeatureQueryService(FeatureRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public FeatureOutput findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(FeatureOutput::from)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found"));
    }

    @Transactional(readOnly = true)
    public List<FeatureOutput> findAll() {
        return repository.findAll().stream().map(FeatureOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureOutput> findByScope(String scopeCode) {
        return repository.findAllByScopes_Code(scopeCode).stream().map(FeatureOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureScopeOutput> findScopes(String identifier) {
        return repository.findByIdentifierWithScopes(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found"))
                .getScopes()
                .stream()
                .map(FeatureScopeOutput::from)
                .toList();
    }
}
