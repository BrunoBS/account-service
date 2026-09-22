package br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LifecycleTypeRepository extends BaseCatalogRepository<LifecycleType> {
}
