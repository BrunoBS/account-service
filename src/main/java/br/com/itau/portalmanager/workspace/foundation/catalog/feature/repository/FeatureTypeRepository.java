package br.com.itau.portalmanager.workspace.foundation.catalog.feature.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.feature.domain.FeatureType;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeatureTypeRepository extends BaseCatalogRepository<FeatureType> {
    boolean existsByNameAndFeatureScopeIdAndIdNot(String name, Long featureScopeId, Long id);
    List<FeatureType> findByNameInAndActiveTrue(List<String> names);
}
