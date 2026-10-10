package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureContextRepository extends JpaRepository<FeatureContext, Long> {
    Optional<FeatureContext> findByIdentifier(String identifier);

    Optional<FeatureContext> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
