package br.com.portalmanager.platform.workspace.core.publisher.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.publisher.domain.PublisherMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.usecase.ResourceScopeTypeService;
import br.com.portalmanager.platform.workspace.core.publisher.repository.PublisherRepository;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.PublisherOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;

@Service
public class PublisherQueryService {
    private final PublisherRepository repository;
    private final PublisherFinder finder;
    private final PublisherNormalizer normalizer;
    private final ResourceScopeTypeService scopeService;
    public PublisherQueryService(PublisherRepository repository, PublisherFinder finder, PublisherNormalizer normalizer,
                                 ResourceScopeTypeService scopeService) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.scopeService = scopeService;
    }

    @Transactional(readOnly = true)
    public PublisherOutput find(String identifier) { return PublisherOutput.from(finder.findActive(identifier)); }

    @Transactional(readOnly = true)
    public List<PublisherOutput> list(Boolean active, String rawScope) {
        String lifecycle = Boolean.FALSE.equals(active) ? LifecycleTypeCode.inactive().value() : LifecycleTypeCode.active().value();
        String scope = normalizer.normalizeScope(rawScope);
        if (scope == null || scope.isBlank()) return repository.findByLifecycle(lifecycle).stream().map(PublisherOutput::from).toList();
        if (Arrays.stream(ResourceScopeTypeEnum.values()).noneMatch(v -> v.name().equals(scope)) || !scopeService.existsActive(scope))
            throw new ValidationException(PublisherMessageKeys.SCOPE_INVALID);
        return repository.findByLifecycleAndScope(lifecycle, scope).stream()
                .map(PublisherOutput::from).toList();
    }
}
