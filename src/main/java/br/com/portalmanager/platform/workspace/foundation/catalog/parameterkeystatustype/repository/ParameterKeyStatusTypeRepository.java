package br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.repository;

import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusType;

import org.springframework.stereotype.Repository;

@Repository
public interface ParameterKeyStatusTypeRepository
        extends CatalogRepository<ParameterKeyStatusType> {}
