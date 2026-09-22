package br.com.itau.portalmanager.workspace.foundation.catalog.schemascopetype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaScopeTypeRepository extends CatalogRepository<SchemaScopeType> {
}
