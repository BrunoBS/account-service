package br.com.itau.portalmanager.workspace.foundation.catalog.authorization.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.authorization.domain.AuthorizationType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizationTypeRepository extends BaseCatalogRepository<AuthorizationType> {
}
