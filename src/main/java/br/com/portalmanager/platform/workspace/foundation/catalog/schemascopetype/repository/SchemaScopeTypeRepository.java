package br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeType;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaScopeTypeRepository extends CatalogRepository<SchemaScopeType> {
}
