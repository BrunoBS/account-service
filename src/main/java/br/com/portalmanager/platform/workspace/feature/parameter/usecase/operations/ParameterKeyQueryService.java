package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKeyMessageKeys;
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

    public ParameterKeyQueryService(
            ParameterKeyRepository repository,
            MicroserviceReferencePort microservices,
            ParameterKeyMapper mapper) {
        this.repository = repository;
        this.microservices = microservices;
        this.mapper = mapper;
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
