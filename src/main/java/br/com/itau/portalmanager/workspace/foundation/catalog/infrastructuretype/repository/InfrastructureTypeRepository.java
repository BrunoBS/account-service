package br.com.itau.portalmanager.workspace.foundation.catalog.infrastructuretype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.infrastructuretype.domain.InfrastructureType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfrastructureTypeRepository extends BaseCatalogRepository<InfrastructureType> {
}
