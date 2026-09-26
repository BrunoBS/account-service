package br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusType;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaVersionStatusTypeRepository extends CatalogRepository<SchemaVersionStatusType> {
}
