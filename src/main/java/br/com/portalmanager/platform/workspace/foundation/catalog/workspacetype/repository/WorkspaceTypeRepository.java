package br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.repository;

import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceTypeRepository extends CatalogRepository<WorkspaceType> {}
