package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.repository.context.ContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.context.ContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.context.ContextValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContextQueryService {
    private final ContextRepository repository;
    private final FeatureRepository features;
    private final ContextFinder finder;
    private final ContextValidator validator;

    public ContextQueryService(ContextRepository repository, FeatureRepository features, ContextFinder finder, ContextValidator validator) {
        this.repository = repository;
        this.features = features;
        this.finder = finder;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public ContextOutput findByIdentifier(String contextIdentifier) {
        return ContextOutput.from(finder.findActive(contextIdentifier));
    }

    @Transactional(readOnly = true)
    public Page<ContextOutput> findAll(String featureIdentifier, String lifecycle, Pageable pageable) {
        validator.validateQuery(pageable);
        String normalizedFeatureIdentifier = featureIdentifier == null || featureIdentifier.isBlank()
            ? null : featureIdentifier.trim();
        if (normalizedFeatureIdentifier != null && !features.existsByIdentifier(normalizedFeatureIdentifier)) {
            throw new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND);
        }
        return repository.findAll(normalizedFeatureIdentifier, LifecycleFilter.resolve(lifecycle).value(), pageable)
            .map(ContextOutput::from);
    }
}
