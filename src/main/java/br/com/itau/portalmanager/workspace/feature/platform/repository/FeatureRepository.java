package br.com.itau.portalmanager.workspace.feature.platform.repository;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;\nimport java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    Optional<Feature> findByIdentifier(String identifier);
    Optional<Feature> findByCode(String code);
    boolean existsByCode(String code);\n    List<Feature> findAllByScopes_Code(String scopeCode);
}
