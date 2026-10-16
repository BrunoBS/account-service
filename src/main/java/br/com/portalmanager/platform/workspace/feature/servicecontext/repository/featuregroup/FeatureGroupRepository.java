package br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeatureGroupRepository extends JpaRepository<FeatureGroup, Long> {
    Optional<FeatureGroup> findByIdentifierAndLifecycle(String identifier, LifecycleTypeCode lifecycle);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select featureGroupEntity from FeatureGroup featureGroupEntity where featureGroupEntity.identifier ="
                    + " :identifier")
    Optional<FeatureGroup> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select featureGroupEntity.id from FeatureGroup featureGroupEntity where"
                    + " featureGroupEntity.identifier = :identifier")
    Optional<Long> findInternalIdByIdentifierForUpdate(@Param("identifier") String identifier);

    Optional<FeatureGroup> findByIdentifier(String identifier);

    Optional<FeatureGroup> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByIdentifier(String identifier);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long excludedId);

    @Query(
            "select featureGroupEntity from FeatureGroup featureGroupEntity where"
                + " featureGroupEntity.lifecycle.value = :lifecycle and (:featureIdentifier is null or"
                + " exists (select relation from FeatureGroupRelation relation where relation.featureGroup"
                + " = featureGroupEntity and relation.feature.identifier = :featureIdentifier))")
    Page<FeatureGroup> findAll(
            @Param("featureIdentifier") String featureIdentifier,
            @Param("lifecycle") String lifecycle,
            Pageable pageable);
}
