package br.com.itau.portalmanager.workspace.foundation.catalog.workspace.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.workspace.domain.WorkspaceType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceTypeRepository extends BaseCatalogRepository<WorkspaceType> {
}
