package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.workspace.feature.platform.domain.*;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelation;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureContextRelationRepository
    extends JpaRepository<FeatureContextRelation, FeatureContextRelationId>
{
    boolean existsByContextId(Long contextId);

    List<FeatureContextRelation> findByContextId(Long contextId);

    @Query(
        "select relation.context from FeatureContextRelation relation where relation.feature.identifier = :featureIdentifier order by relation.context.id"
    )
    List<FeatureContext> findContextsByFeatureIdentifier(@Param("featureIdentifier") String featureIdentifier);

    @Query(
        "select relation.feature from FeatureContextRelation relation where relation.context.code = :contextCode order by relation.feature.id"
    )
    List<Feature> findFeaturesByContextCode(@Param("contextCode") String contextCode);
}
