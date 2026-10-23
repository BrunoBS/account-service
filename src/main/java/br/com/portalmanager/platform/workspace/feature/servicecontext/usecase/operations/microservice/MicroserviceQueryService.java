package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.microservice.projection.MicroserviceReferenceProjection;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.microservice.MicroserviceMapper;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.MicroserviceReference;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.microservice.MicroserviceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MicroserviceQueryService {

    private final MicroserviceRepository repository;
    private final MicroserviceFinder finder;
    private final MicroserviceMapper mapper;
    private final MicroserviceValidator validator;

    public MicroserviceQueryService(
            MicroserviceRepository repository,
            MicroserviceFinder finder,
            MicroserviceMapper mapper,
            MicroserviceValidator validator) {
        this.repository = repository;
        this.finder = finder;
        this.mapper = mapper;
        this.validator = validator;
    }

    /** The shared lock keeps this reference active until the consumer transaction completes. */
    @Transactional
    public Optional<Long> findActiveReferenceIdByIdentifier(String identifier) {
        return repository.findActiveIdForReference(identifier);
    }

    @Transactional(readOnly = true)
    public MicroserviceReference findReferenceByIdentifier(String identifier) {
        return repository
                .findReferenceByIdentifier(identifier)
                .map(this::toReference)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ServiceContextMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<MicroserviceReference> findReferenceByCode(String code) {
        return repository.findReferenceByCode(code).map(this::toReference);
    }

    @Transactional(readOnly = true)
    public Map<Long, MicroserviceReference> findReferencesByInternalIds(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return repository.findReferencesByInternalIds(ids).stream()
                .map(this::toReference)
                .collect(Collectors.toMap(MicroserviceReference::id, reference -> reference));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findIdentifiersByInternalIds(Collection<Long> microserviceIds) {
        if (microserviceIds.isEmpty()) return Map.of();
        return repository.findIdentifierReferences(microserviceIds).stream()
                .collect(
                        Collectors.toMap(
                                MicroserviceRepository.IdentifierReference::getId,
                                MicroserviceRepository.IdentifierReference::getIdentifier));
    }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null
                && repository.existsByCodeAndLifecycle(code, LifecycleTypeCode.active());
    }

    @Transactional(readOnly = true)
    public boolean existsActiveByIdentifier(String identifier) {
        return identifier != null
                && repository.existsByIdentifierAndLifecycle(
                        identifier, LifecycleTypeCode.active());
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByIdentifier(String identifier) {
        return repository
                .findInternalIdByIdentifier(identifier)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ServiceContextMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Long findActiveInternalIdByIdentifier(String identifier) {
        return repository
                .findActiveInternalIdByIdentifier(identifier)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ServiceContextMessageKeys.ACTIVE_MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long microserviceId) {
        return repository
                .findIdentifierByInternalId(microserviceId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ServiceContextMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MicroserviceOutput findByIdentifier(String identifier) {
        return mapper.toOutput(finder.findActive(identifier));
    }

    @Transactional(readOnly = true)
    public Page<MicroserviceOutput> findAll(String lifecycle, Pageable pageable) {
        validator.validateQuery(pageable);
        return repository
                .findByLifecycle(LifecycleFilter.resolve(lifecycle), pageable)
                .map(mapper::toOutput);
    }

    private MicroserviceReference toReference(MicroserviceReferenceProjection reference) {
        return new MicroserviceReference(
                reference.getId(), reference.getIdentifier(), reference.getCode());
    }
}
