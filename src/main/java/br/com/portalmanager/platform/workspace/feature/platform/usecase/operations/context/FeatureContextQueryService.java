package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<FeatureContextOutput> findAll() {
        return repository.findAll().stream().map(FeatureContextOutput::from).toList();
    }
}
