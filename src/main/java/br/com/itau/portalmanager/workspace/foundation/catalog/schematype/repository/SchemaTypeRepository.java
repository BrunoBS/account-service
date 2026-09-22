package br.com.itau.portalmanager.workspace.foundation.catalog.schematype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaTypeRepository extends BaseCatalogRepository<SchemaType> {
    boolean existsByNameAndSchemaScopeNameAndIdNot(String name, String scopeName, Long id);
    Optional<SchemaType> findByNameAndSchemaScopeNameAndActiveTrue(String name, String scopeName);
    List<SchemaType> findBySchemaScopeNameAndActiveTrueOrderBySortOrderAsc(String scopeName);
}
