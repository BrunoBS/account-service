package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select feature from Feature feature where feature.identifier = :identifier")
    Optional<Feature> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Query("select feature.identifier from Feature feature where feature.id = :featureId")
    Optional<String> findIdentifierById(@Param("featureId") Long featureId);

    List<Feature> findByIdIn(Collection<Long> featureIds);

    Optional<Feature> findByIdentifier(String identifier);

    Optional<Feature> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
