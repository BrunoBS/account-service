package br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.domain.FeatureType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeatureTypeRepository extends BaseCatalogRepository<FeatureType> {
    List<FeatureType> findByNameInAndActiveTrue(List<String> names);
}
