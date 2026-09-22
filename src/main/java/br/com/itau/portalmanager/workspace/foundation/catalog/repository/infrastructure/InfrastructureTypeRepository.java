package br.com.itau.portalmanager.workspace.foundation.catalog.repository.infrastructure;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure.InfrastructureType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfrastructureTypeRepository extends BaseCatalogRepository<InfrastructureType> {
}
