package br.com.itau.portalmanager.workspace.foundation.catalog.servicetype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.servicetype.domain.ServiceType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceTypeRepository extends CatalogRepository<ServiceType> {
}
