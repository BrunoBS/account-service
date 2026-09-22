package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.feature;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.feature.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.featurescope.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.feature.FeatureTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.featurescope.FeatureScopeTypeRepository;
import br.com.portalmanager.platform.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.catalog.service.BaseCatalogService;
import br.com.portalmanager.platform.messaging.exception.NotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FeatureTypeService extends BaseCatalogService<FeatureType, FeatureTypeDTO> {

    private final FeatureTypeRepository repository;
    private final FeatureScopeTypeRepository scopeRepository;

    public FeatureTypeService(
            FeatureTypeRepository repository,
            FeatureTypeMapper mapper,
            FeatureTypeValidator validator,
            FeatureScopeTypeRepository scopeRepository) {
        super(repository, mapper, validator);
        this.repository = repository;
        this.scopeRepository = scopeRepository;
    }

    public List<FeatureType> findActiveByNames(List<String> names) {
        return repository.findByNameInAndActiveTrue(names);
    }

    @Override
    protected void applyAdditionalFields(FeatureType entity, FeatureTypeDTO dto) {
        FeatureScopeType scope = scopeRepository.findByIdAndActiveTrue(dto.featureScopeId())
                .orElseThrow(() -> new NotFoundException(
                        CatalogMessageKeys.NOT_FOUND,
                        Map.of("0", FeatureScopeType.class.getSimpleName())
                ));
        entity.setFeatureScope(scope);
        entity.setAvailable(Boolean.TRUE.equals(dto.available()));
    }

    @Override protected Set<String> additionalAllowedFilters() {
        return Set.of("featureScopeId", "featureScopeName", "available");
    }

    @Override
    protected Specification<FeatureType> additionalSpecification(Map<String, String> filters) {
        Specification<FeatureType> specification = null;

        String scopeId = filters.get("featureScopeId");
        if (scopeId != null && !scopeId.isBlank()) {
            try {
                Long id = Long.valueOf(scopeId);
                specification = and(specification,
                        (root, query, cb) -> cb.equal(root.get("featureScope").get("id"), id));
            } catch (NumberFormatException exception) {
                throw invalidFilterException("featureScopeId", scopeId, "long");
            }
        }

        String scopeName = filters.get("featureScopeName");
        if (scopeName != null && !scopeName.isBlank()) {
            specification = and(specification,
                    (root, query, cb) -> cb.equal(root.get("featureScope").get("name"), scopeName));
        }

        String available = filters.get("available");
        if (available != null && !available.isBlank()) {
            boolean value = booleanFilter(filters, "available", false);
            specification = and(specification,
                    (root, query, cb) -> cb.equal(root.get("available"), value));
        }
        return specification;
    }

    private Specification<FeatureType> and(
            Specification<FeatureType> current,
            Specification<FeatureType> next) {
        return current == null ? next : current.and(next);
    }
}
