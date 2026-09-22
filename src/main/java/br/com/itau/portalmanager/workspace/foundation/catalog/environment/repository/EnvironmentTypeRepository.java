package br.com.itau.portalmanager.workspace.foundation.catalog.environment.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.environment.domain.EnvironmentType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentTypeRepository extends BaseCatalogRepository<EnvironmentType> {
}
