package br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.repository;

import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import org.springframework.stereotype.Repository;

@Repository
public interface LifecycleTypeRepository extends CatalogRepository<LifecycleType> {}
