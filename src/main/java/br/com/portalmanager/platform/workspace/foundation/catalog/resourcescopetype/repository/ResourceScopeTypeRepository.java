package br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.repository;

import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeType;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceScopeTypeRepository extends CatalogRepository<ResourceScopeType> {
}
