package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKeyMessageKeys;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.parameter.config.ParameterProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterCachePage;
import org.springframework.transaction.annotation.Propagation;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyReadProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.mapping.ParameterKeyMapper;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyOutput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort.Reference;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyQueryValidation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ParameterKeyQueryService {
    private final ParameterKeyRepository repository;
    private final MicroserviceReferencePort microservices;
    private final ParameterKeyMapper mapper;
    private final ParameterPublicationStore publicationStore;
    private final ParameterProperties properties;

    public ParameterKeyQueryService(
            ParameterKeyRepository repository,
            MicroserviceReferencePort microservices,
            ParameterKeyMapper mapper,
            ParameterPublicationStore publicationStore,
            ParameterProperties properties) {
        this.repository = repository;
        this.microservices = microservices;
        this.mapper = mapper;
        this.publicationStore = publicationStore;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ParameterCachePage findCache(String microserviceIdentifier, String cursor, Integer count) {
        ParameterKeyQueryValidation.requireCacheMicroservice(microserviceIdentifier);
        int scanCount = count == null ? properties.runtime().batchSize() : count;
        if (scanCount < 1 || scanCount > properties.runtime().batchSize()) {
            throw new ValidationException(ParameterKeyMessageKeys.PAGE_INVALID);
        }
        Reference reference = microservices.findByIdentifier(microserviceIdentifier);
        return publicationStore.scan(reference.code(), cursor, scanCount);
    }

    @Transactional(readOnly = true)
    public ParameterKeyOutput findByIdentifier(String identifier) {
        ParameterKeyReadProjection resource =
                repository
                        .findProjection(identifier)
                        .orElseThrow(
                                () -> new NotFoundException(ParameterKeyMessageKeys.NOT_FOUND));
        Reference reference =
                microservices
                        .findByInternalIds(List.of(resource.getMicroserviceId()))
                        .get(resource.getMicroserviceId());
        return mapper.toOutput(resource, reference);
    }

    @Transactional(readOnly = true)
    public Page<ParameterKeyOutput> findAll(
            String microserviceIdentifier,
            String status,
            String type,
            boolean active,
            String key,
            Pageable pageable) {
        ParameterKeyQueryValidation.validatePage(pageable);
        String normalizedStatus = ParameterKeyQueryValidation.resolveStatus(status);
        String normalizedType = ParameterKeyQueryValidation.resolveType(type);
        Long microserviceId =
                microserviceIdentifier == null
                        ? null
                        : microservices.findByIdentifier(microserviceIdentifier).internalId();
        Sort sort =
                pageable.getSort().getOrderFor("id") == null
                        ? pageable.getSort().and(Sort.by("id"))
                        : pageable.getSort();
        Pageable stablePage =
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<ParameterKeyReadProjection> page =
                repository.findPage(
                        microserviceId,
                        normalizedStatus,
                        normalizedType,
                        active,
                        key == null || key.isBlank() ? null : key.trim(),
                        stablePage);
        Set<Long> ids =
                page.stream()
                        .map(ParameterKeyReadProjection::getMicroserviceId)
                        .collect(Collectors.toSet());
        Map<Long, Reference> references = microservices.findByInternalIds(ids);
        return page.map(
                resource ->
                        mapper.toOutput(resource, references.get(resource.getMicroserviceId())));
    }
}
