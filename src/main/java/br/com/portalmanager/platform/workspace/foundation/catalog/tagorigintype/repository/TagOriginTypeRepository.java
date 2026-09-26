package br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain.TagOriginType;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagOriginTypeRepository extends CatalogRepository<TagOriginType> {
}
