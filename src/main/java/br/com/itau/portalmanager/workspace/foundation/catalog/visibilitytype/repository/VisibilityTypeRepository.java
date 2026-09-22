package br.com.itau.portalmanager.workspace.foundation.catalog.visibilitytype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.visibilitytype.domain.VisibilityType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisibilityTypeRepository extends BaseCatalogRepository<VisibilityType> {
}
