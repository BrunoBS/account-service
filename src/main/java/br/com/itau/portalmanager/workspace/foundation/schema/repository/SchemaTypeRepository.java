package br.com.itau.portalmanager.workspace.foundation.schema.repository;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaTypeRepository extends CatalogRepository<SchemaType> {
}
