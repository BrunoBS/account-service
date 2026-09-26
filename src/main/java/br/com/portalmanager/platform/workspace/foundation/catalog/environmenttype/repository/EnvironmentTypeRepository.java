package br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.domain.EnvironmentType;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentTypeRepository extends CatalogRepository<EnvironmentType> {
}
