package br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationScopeTypeRepository extends CatalogRepository<ApplicationScopeType> {
}
