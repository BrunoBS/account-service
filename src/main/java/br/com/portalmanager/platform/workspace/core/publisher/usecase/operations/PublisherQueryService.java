package br.com.portalmanager.platform.workspace.core.publisher.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.publisher.domain.PublisherMessageKeys;
import br.com.portalmanager.platform.workspace.core.publisher.domain.PublisherScope;
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
    public PublisherQueryService(PublisherRepository repository, PublisherFinder finder, PublisherNormalizer normalizer) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
    }

    @Transactional(readOnly = true)
    public PublisherOutput find(String identifier) { return PublisherOutput.from(finder.findActive(identifier)); }

    @Transactional(readOnly = true)
    public List<PublisherOutput> list(Boolean active, String rawScope) {
        String lifecycle = Boolean.FALSE.equals(active) ? LifecycleTypeCode.inactive().value() : LifecycleTypeCode.active().value();
        String scope = normalizer.normalizeScope(rawScope);
        if (scope == null || scope.isBlank()) return repository.findByLifecycle(lifecycle).stream().map(PublisherOutput::from).toList();
        if (Arrays.stream(PublisherScope.values()).noneMatch(v -> v.name().equals(scope)))
            throw new ValidationException(PublisherMessageKeys.SCOPE_INVALID);
        return repository.findByLifecycleAndScope(lifecycle, PublisherScope.valueOf(scope)).stream()
                .map(PublisherOutput::from).toList();
    }
}
