package br.com.itau.portalmanager.workspace.feature.platform.repository;

import br.com.itau.portalmanager.workspace.feature.platform.domain.FeatureContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureContextRepository extends JpaRepository<FeatureContext, Long> {
    Optional<FeatureContext> findByIdentifier(String identifier);
    Optional<FeatureContext> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
