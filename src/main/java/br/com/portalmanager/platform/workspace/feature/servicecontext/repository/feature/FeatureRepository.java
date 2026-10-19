package br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    @EntityGraph(attributePaths = "microservice")
    @Query(
            "select feature from Feature feature where feature.lifecycle.value = :lifecycle and"
                + " (:featureGroupIdentifier is null or exists (select relation from FeatureGroupRelation"
                + " relation where relation.feature = feature and relation.featureGroup.identifier ="
                + " :featureGroupIdentifier))")
    Page<Feature> findAll(
            @Param("featureGroupIdentifier") String featureGroupIdentifier,
            @Param("lifecycle") String lifecycle,
            Pageable pageable);

    @EntityGraph(attributePaths = "microservice")
    Optional<Feature> findByIdentifierAndLifecycle(String identifier, LifecycleTypeCode lifecycle);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select feature.id from Feature feature where feature.identifier = :identifier")
    Optional<Long> findInternalIdByIdentifierForUpdate(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select feature from Feature feature where feature.identifier = :identifier")
    Optional<Feature> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Query("select feature.id from Feature feature where feature.identifier = :identifier")
    Optional<Long> findInternalIdByIdentifier(@Param("identifier") String identifier);

    @Query(
            "select feature.microservice.identifier from Feature feature where feature.identifier ="
                    + " :identifier")
    Optional<String> findMicroserviceIdentifierByIdentifier(@Param("identifier") String identifier);

    @Query("select feature.identifier from Feature feature where feature.id = :featureId")
    Optional<String> findIdentifierById(@Param("featureId") Long featureId);

    @Query(
            "select feature.id as id, feature.identifier as identifier, feature.name as name from"
                    + " Feature feature where feature.id in :ids")
    List<FeatureReference> findReferences(@Param("ids") Collection<Long> ids);

    @Query(
            "select feature.id as id, feature.identifier as identifier, feature.name as name from"
                + " Feature feature where feature.id in :ids and feature.lifecycle.value = 'ACTIVE'"
                + " and feature.shareable = true and feature.microservice.lifecycle.value ="
                + " 'ACTIVE'")
    List<FeatureReference> findAvailableReferencesForResourceSharing(
            @Param("ids") Collection<Long> ids);

    interface FeatureReference {
        Long getId();

        String getIdentifier();

        String getName();
    }

    Optional<Feature> findByIdentifier(String identifier);

    Optional<Feature> findByCode(String code);

    boolean existsByIdentifier(String identifier);

    boolean existsByIdentifierAndLifecycle(String identifier, LifecycleTypeCode lifecycle);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long excludedId);

    boolean existsByMicroservice_Id(Long microserviceId);
}
