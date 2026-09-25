package br.com.itau.portalmanager.workspace.feature.platform.usecase.feature;

import br.com.itau.portalmanager.workspace.feature.platform.repository.FeatureRepository;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.FeatureContextOutput;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.model.FeatureOutput;
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
    public List<FeatureOutput> findByContext(String contextCode) {
        return repository.findAllByContexts_Code(contextCode).stream().map(FeatureOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureContextOutput> findContexts(String identifier) {
        return repository.findByIdentifierWithContexts(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found"))
                .getContexts()
                .stream()
                .map(FeatureContextOutput::from)
                .toList();
    }
}
