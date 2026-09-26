package br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain.VisibilityType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisibilityTypeRepository extends CatalogRepository<VisibilityType> {
}
