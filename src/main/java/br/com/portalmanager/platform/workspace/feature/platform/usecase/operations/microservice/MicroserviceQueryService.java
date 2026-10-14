package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.microservice.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.microservice.MicroserviceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroserviceQueryService {

    private final MicroserviceRepository repository;
    private final MicroserviceFinder finder;
    private final MicroserviceMapper mapper;
    private final MicroserviceValidator validator;

    public MicroserviceQueryService(
        MicroserviceRepository repository, MicroserviceFinder finder,
        MicroserviceMapper mapper, MicroserviceValidator validator
    ) {
        this.repository = repository;
        this.finder = finder;
        this.mapper = mapper;
        this.validator = validator;
    }

    /** The shared lock keeps this reference active until the message transaction completes. */
    @Transactional
    public Optional<Long> findActiveInternalIdForMessage(String identifier) {
        return repository.findActiveIdForReference(identifier);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findIdentifiersByInternalIds(Collection<Long> microserviceIds) {
        if (microserviceIds.isEmpty()) return Map.of();
        return repository.findIdentifierReferences(microserviceIds).stream()
            .collect(Collectors.toMap(MicroserviceRepository.IdentifierReference::getId,
                MicroserviceRepository.IdentifierReference::getIdentifier));
    }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null && repository.existsByCodeAndLifecycle(code, LifecycleTypeCode.active());
    }

    @Transactional(readOnly = true)
    public boolean existsActiveByIdentifier(String identifier) {
        return identifier != null && repository.existsByIdentifierAndLifecycle(identifier, LifecycleTypeCode.active());
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByIdentifier(String identifier) {
        return repository.findInternalIdByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Long findActiveInternalIdByIdentifier(String identifier) {
        return repository.findActiveInternalIdByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.ACTIVE_MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long microserviceId) {
        return repository.findIdentifierByInternalId(microserviceId)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MicroserviceOutput findByIdentifier(String identifier) {
        return mapper.toOutput(finder.findActive(identifier));
    }

    @Transactional(readOnly = true)
    public Page<MicroserviceOutput> findAll(String lifecycle, Pageable pageable) {
        validator.validateQuery(pageable);
        return repository.findByLifecycle(LifecycleFilter.resolve(lifecycle), pageable).map(mapper::toOutput);
    }
}
