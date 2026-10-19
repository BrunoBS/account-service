package br.com.portalmanager.platform.workspace.core.publisher.usecase.operations;

import br.com.portalmanager.platform.workspace.core.publisher.repository.PublisherRepository;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.mapping.PublisherMapper;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.validation.PublisherValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublisherQueryService {

    private final PublisherRepository repository;
    private final PublisherFinder finder;
    private final PublisherNormalizer normalizer;
    private final PublisherValidator validator;
    private final PublisherMapper mapper;

    public PublisherQueryService(
        PublisherRepository repository,
        PublisherFinder finder,
        PublisherNormalizer normalizer,
        PublisherValidator validator,
        PublisherMapper mapper
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PublisherOutput find(String identifier) {
        return mapper.toOutput(finder.findActive(identifier));
    }

    @Transactional(readOnly = true)
    public Page<PublisherOutput> findAll(String lifecycle, String rawScope, Pageable pageable) {
        String scope = normalizer.normalizeScope(rawScope);
        validator.validateQuery(scope, pageable);
        String lifecycleCode = LifecycleFilter.resolve(lifecycle).value();
        if (scope == null || scope.isBlank()) return repository
            .findByLifecycle(lifecycleCode, pageable)
            .map(mapper::toOutput);
        return repository.findByLifecycleAndScope(lifecycleCode, scope, pageable).map(mapper::toOutput);
    }
}
