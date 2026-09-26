package br.com.itau.portalmanager.workspace.foundation.catalog.schematype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaTypeRepository extends CatalogRepository<SchemaType> {
}
