package br.com.itau.portalmanager.workspace.foundation.catalog.repository.environment;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.environment.EnvironmentType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentTypeRepository extends BaseCatalogRepository<EnvironmentType> {
}
