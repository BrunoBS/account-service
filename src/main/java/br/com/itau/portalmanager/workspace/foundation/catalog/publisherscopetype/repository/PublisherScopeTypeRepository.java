package br.com.itau.portalmanager.workspace.foundation.catalog.publisherscopetype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.publisherscopetype.domain.PublisherScopeType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublisherScopeTypeRepository extends CatalogRepository<PublisherScopeType> {
}
