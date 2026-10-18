package br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroupRelation;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroupRelationId;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeatureGroupRelationRepository
        extends JpaRepository<FeatureGroupRelation, FeatureGroupRelationId> {
    /** Both parents must be locked before this insert; only the duplicate pair can be ignored. */
    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    "insert ignore into feature_group_feature (feature_id, feature_group_id) "
                            + "values (:featureId, :featureGroupId)",
            nativeQuery = true)
    int insertIfAbsent(@Param("featureId") Long featureId, @Param("featureGroupId") Long featureGroupId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<RelationIdReference> findFirstByFeatureGroupId(Long featureGroupId);

    interface RelationIdReference {
        FeatureGroupRelationId getId();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select relation from FeatureGroupRelation relation where relation.feature.id ="
                    + " :featureId and relation.featureGroup.id = :featureGroupId")
    Optional<FeatureGroupRelation> findByPairForUpdate(
            @Param("featureId") Long featureId, @Param("featureGroupId") Long featureGroupId);
}
