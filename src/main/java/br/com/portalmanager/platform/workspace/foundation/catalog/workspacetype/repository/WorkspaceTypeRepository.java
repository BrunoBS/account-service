package br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceTypeRepository extends CatalogRepository<WorkspaceType> {
}
