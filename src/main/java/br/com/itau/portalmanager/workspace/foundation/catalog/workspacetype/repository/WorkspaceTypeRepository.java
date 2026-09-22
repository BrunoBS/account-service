package br.com.itau.portalmanager.workspace.foundation.catalog.workspacetype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceTypeRepository extends BaseCatalogRepository<WorkspaceType> {
}
