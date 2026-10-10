package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class FeatureQueryService {

    private final FeatureRepository repository;

    public FeatureQueryService(FeatureRepository repository) {
        this.repository = repository;
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
        return repository.findAllByContexts_Code(contextCode).stream().map(FeatureOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureContextOutput> findContexts(String identifier) {
        return repository
            .findByIdentifierWithContexts(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND))
            .getContexts()
            .stream()
            .map(FeatureContextOutput::from)
            .toList();
    }
}
