package br.com.itau.portalmanager.workspace.foundation.catalog.environmenttype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.environmenttype.domain.EnvironmentType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentTypeRepository extends CatalogRepository<EnvironmentType> {
}
