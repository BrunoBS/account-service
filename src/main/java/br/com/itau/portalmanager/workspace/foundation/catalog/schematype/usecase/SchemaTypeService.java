package br.com.itau.portalmanager.workspace.foundation.catalog.schematype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.domain.SchemaScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.repository.SchemaScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.repository.SchemaTypeRepository;
import br.com.portalmanager.platform.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.catalog.service.BaseCatalogService;
import br.com.portalmanager.platform.messaging.exception.NotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class SchemaTypeService extends BaseCatalogService<SchemaType, SchemaTypeDTO> {

    private final SchemaTypeRepository repository;
    private final SchemaScopeTypeRepository scopeRepository;

    public SchemaTypeService(
            SchemaTypeRepository repository,
            SchemaTypeMapper mapper,
            SchemaTypeValidator validator,
            SchemaScopeTypeRepository scopeRepository) {
        super(repository, mapper, validator);
        this.repository = repository;
        this.scopeRepository = scopeRepository;
    }

    public SchemaType findByNameAndScope(String name, String scope) {
        return repository.findByNameAndSchemaScopeNameAndActiveTrue(name, scope)
                .orElseThrow(() -> new NotFoundException(
                        CatalogMessageKeys.NOT_FOUND,
                        Map.of("0", SchemaType.class.getSimpleName())
                ));
    }

    @Override
    protected void applyAdditionalFields(SchemaType entity, SchemaTypeDTO dto) {
        SchemaScopeType scope = scopeRepository.findByNameAndActiveTrue(dto.scope())
                .orElseThrow(() -> new NotFoundException(
                        CatalogMessageKeys.NOT_FOUND,
                        Map.of("0", SchemaScopeType.class.getSimpleName())
                ));
        entity.setSchemaScope(scope);
    }

    @Override protected Set<String> additionalAllowedFilters() { return Set.of("scope"); }

    @Override
    protected Specification<SchemaType> additionalSpecification(Map<String, String> filters) {
        String scope = filters.get("scope");
        if (scope == null || scope.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("schemaScope").get("name"), scope);
    }
}
