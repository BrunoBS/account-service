package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContext;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureContextRepository extends JpaRepository<FeatureContext, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select context from FeatureContext context where context.identifier = :identifier")
    Optional<FeatureContext> findByIdentifierForUpdate(@Param("identifier") String identifier);

    Optional<FeatureContext> findByIdentifier(String identifier);

    Optional<FeatureContext> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
