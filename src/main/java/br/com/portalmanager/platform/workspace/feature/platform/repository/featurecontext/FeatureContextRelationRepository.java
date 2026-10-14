package br.com.portalmanager.platform.workspace.feature.platform.repository.featurecontext;

import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelation;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelationId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureContextRelationRepository extends JpaRepository<FeatureContextRelation, FeatureContextRelationId> {
    /** Both parents must be locked before this insert; only the duplicate pair can be ignored. */
    @Modifying(flushAutomatically = true)
    @Query(value = "insert ignore into platform_feature_context_relations (feature_id, feature_context_id) " +
        "values (:featureId, :contextId)", nativeQuery = true)
    int insertIfAbsent(@Param("featureId") Long featureId, @Param("contextId") Long contextId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<RelationIdReference> findFirstByContextId(Long contextId);

    interface RelationIdReference {
        FeatureContextRelationId getId();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select relation from FeatureContextRelation relation where relation.feature.id = :featureId and relation.context.id = :contextId")
    Optional<FeatureContextRelation> findByPairForUpdate(@Param("featureId") Long featureId, @Param("contextId") Long contextId);
}
