package br.com.itau.portalmanager.workspace.foundation.catalog.repository.authorization;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.authorization.AuthorizationType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizationTypeRepository extends BaseCatalogRepository<AuthorizationType> {
}
