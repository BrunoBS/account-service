package br.com.itau.portalmanager.workspace.foundation.catalog.resourcescopetype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceScopeTypeRepository extends CatalogRepository<ResourceScopeType> {
}
