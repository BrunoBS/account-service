package br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;

import jakarta.persistence.*;

@Entity
@Table(name = "feature_group_feature")
public class FeatureGroupRelation {

    @EmbeddedId private FeatureGroupRelationId id;

    @MapsId("featureId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private Feature feature;

    @MapsId("featureGroupId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_group_id", nullable = false)
    private FeatureGroup featureGroup;

    protected FeatureGroupRelation() {}

    public FeatureGroupRelation(Feature feature, FeatureGroup featureGroupEntity) {
        this.feature = feature;
        this.featureGroup = featureGroupEntity;
        id = new FeatureGroupRelationId(feature.getId(), featureGroupEntity.getId());
    }

    public FeatureGroupRelationId getId() {
        return id;
    }

    public Feature getFeature() {
        return feature;
    }

    public FeatureGroup getFeatureGroup() {
        return featureGroup;
    }
}
