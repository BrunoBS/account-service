package br.com.itau.portalmanager.workspace.foundation.catalog.sharestatus.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.sharestatus.domain.ShareStatusType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareStatusTypeRepository extends BaseCatalogRepository<ShareStatusType> {
}
