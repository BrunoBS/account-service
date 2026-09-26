package br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain.ShareStatusType;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareStatusTypeRepository extends CatalogRepository<ShareStatusType> {
}
