package br.com.itau.portalmanager.workspace.foundation.catalog.repository.lifecycle;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.lifecycle.LifecycleType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LifecycleTypeRepository extends BaseCatalogRepository<LifecycleType> {
}
