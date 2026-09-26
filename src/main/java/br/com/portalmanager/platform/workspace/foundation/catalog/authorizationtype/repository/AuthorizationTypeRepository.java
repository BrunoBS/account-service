package br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.repository;

import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationType;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizationTypeRepository extends CatalogRepository<AuthorizationType> {
}
