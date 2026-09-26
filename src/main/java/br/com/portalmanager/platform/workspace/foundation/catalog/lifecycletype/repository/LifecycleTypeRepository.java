package br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LifecycleTypeRepository extends CatalogRepository<LifecycleType> {
}
