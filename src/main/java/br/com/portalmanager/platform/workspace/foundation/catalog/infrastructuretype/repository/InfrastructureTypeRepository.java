package br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.domain.InfrastructureType;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfrastructureTypeRepository extends CatalogRepository<InfrastructureType> {
}
