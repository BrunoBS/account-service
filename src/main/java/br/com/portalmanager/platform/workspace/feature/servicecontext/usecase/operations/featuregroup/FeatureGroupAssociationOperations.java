package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroupRelation;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRelationRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.FeatureGroupAssociationOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.feature.FeatureMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Only actual changes reach these audited operations; retries return the existing representation.
 */
@Service
public class FeatureGroupAssociationOperations {
    private final FeatureGroupRelationRepository relations;
    private final FeatureRepository features;
    private final FeatureMapper mapper;

    public FeatureGroupAssociationOperations(
            FeatureGroupRelationRepository relations,
            FeatureRepository features,
            FeatureMapper mapper) {
        this.relations = relations;
        this.features = features;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "FEATURE_FEATURE_GROUP_ASSOCIATED",
            resourceType = "FEATURE")
    public FeatureGroupAssociationOutput associate(Feature feature, FeatureGroup featureGroupEntity) {
        // The command has inserted the pair in this transaction; only real changes reach auditing.
        feature.markUpdated(LocalDateTime.now());
        Feature savedFeature = features.saveAndFlush(feature);
        return mapper.toAssociationOutput(savedFeature, featureGroupEntity.getIdentifier());
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "FEATURE_FEATURE_GROUP_REMOVED",
            resourceType = "FEATURE")
    public FeatureGroupAssociationOutput remove(Feature feature, FeatureGroupRelation relation) {
        String featureGroupIdentifier = relation.getFeatureGroup().getIdentifier();
        relations.delete(relation);
        feature.markUpdated(LocalDateTime.now());
        Feature savedFeature = features.saveAndFlush(feature);
        return mapper.toAssociationOutput(savedFeature, featureGroupIdentifier);
    }
}
