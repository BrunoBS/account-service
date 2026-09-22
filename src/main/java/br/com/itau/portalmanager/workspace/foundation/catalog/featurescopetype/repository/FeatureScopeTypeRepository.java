package br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureScopeTypeRepository extends CatalogRepository<FeatureScopeType> {
}
