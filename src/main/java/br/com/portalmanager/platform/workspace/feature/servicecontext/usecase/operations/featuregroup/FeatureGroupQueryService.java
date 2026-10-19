package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.FeatureGroupOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.featuregroup.FeatureGroupValidator;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.featuregroup.FeatureGroupMapper;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureGroupQueryService {
    private final FeatureGroupRepository repository;
    private final FeatureRepository features;
    private final FeatureGroupFinder finder;
    private final FeatureGroupValidator validator;
    private final FeatureGroupMapper mapper;

    public FeatureGroupQueryService(
            FeatureGroupRepository repository,
            FeatureRepository features,
            FeatureGroupFinder finder,
            FeatureGroupValidator validator,
            FeatureGroupMapper mapper) {
        this.repository = repository;
        this.features = features;
        this.finder = finder;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public FeatureGroupOutput findByIdentifier(String featureGroupIdentifier) {
        return mapper.toOutput(finder.findActive(featureGroupIdentifier));
    }

    @Transactional(readOnly = true)
    public Page<FeatureGroupOutput> findAll(
            String featureIdentifier, String lifecycle, Pageable pageable) {
        validator.validateQuery(pageable);
        String normalizedFeatureIdentifier =
                featureIdentifier == null || featureIdentifier.isBlank()
                        ? null
                        : featureIdentifier.trim();
        if (normalizedFeatureIdentifier != null
                && !features.existsByIdentifier(normalizedFeatureIdentifier)) {
            throw new NotFoundException(ServiceContextMessageKeys.FEATURE_NOT_FOUND);
        }
        return repository
                .findAll(
                        normalizedFeatureIdentifier,
                        LifecycleFilter.resolve(lifecycle).value(),
                        pageable)
                .map(mapper::toOutput);
    }
}
