package br.com.portalmanager.platform.workspace.foundation.catalog.schematype.repository;

import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.schematype.domain.SchemaType;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaTypeRepository extends CatalogRepository<SchemaType> {
}
