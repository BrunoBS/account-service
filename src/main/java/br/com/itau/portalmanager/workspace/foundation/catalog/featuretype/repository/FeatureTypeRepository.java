package br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.domain.FeatureType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeatureTypeRepository extends CatalogRepository<FeatureType> {
    List<FeatureType> findByCodeInAndActiveTrue(List<String> codes);
}
