package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.domain.parameterkey.ParameterKeyMessageKeys;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter.Reference;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.projection.ParameterKeyReadProjection;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.validation.parameterkey.ParameterKeyQueryValidation;

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
    private final DynamicParameterMicroserviceAdapter microservices;

    public ParameterKeyQueryService(
            ParameterKeyRepository repository, DynamicParameterMicroserviceAdapter microservices) {
        this.repository = repository;
        this.microservices = microservices;
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
        return toOutput(resource, reference);
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
                resource -> toOutput(resource, references.get(resource.getMicroserviceId())));
    }

    private ParameterKeyOutput toOutput(ParameterKeyReadProjection resource, Reference reference) {
        return new ParameterKeyOutput(
                resource.getVersion(),
                resource.getIdentifier(),
                reference.identifier(),
                resource.getKey(),
                reference.code() + "." + resource.getKey(),
                resource.getDescription(),
                resource.getType(),
                resource.getValue().deepCopy(),
                resource.getStatus(),
                resource.isActive(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
