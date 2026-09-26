package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context;

import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
public class FeatureContextQueryService {

    private final FeatureContextRepository repository;

    public FeatureContextQueryService(FeatureContextRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public FeatureContextOutput findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(FeatureContextOutput::from)
                .orElseThrow(() -> new IllegalArgumentException("Feature context not found"));
    }

    @Transactional(readOnly = true)
    public List<FeatureContextOutput> findAll() {
        return repository.findAll().stream().map(FeatureContextOutput::from).toList();
    }
}
