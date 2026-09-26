package br.com.portalmanager.platform.workspace.foundation.catalog.schematype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaTypeRepository extends CatalogRepository<SchemaType> {
}
