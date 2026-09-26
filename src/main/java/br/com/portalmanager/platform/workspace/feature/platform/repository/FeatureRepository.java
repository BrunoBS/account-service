package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    Optional<Feature> findByIdentifier(String identifier);

    Optional<Feature> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    List<Feature> findAllByContexts_Code(String contextCode);

    @Query("""
            select distinct f
              from Feature f
              left join fetch f.contexts
             where f.identifier = :identifier
            """)
    Optional<Feature> findByIdentifierWithContexts(@Param("identifier") String identifier);
}
