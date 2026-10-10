package br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext;

import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import jakarta.persistence.*;

@Entity
@Table(name = "platform_feature_context_relations")
public class FeatureContextRelation {

    @EmbeddedId
    private FeatureContextRelationId id;

    @MapsId("featureId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private Feature feature;

    @MapsId("featureContextId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_context_id", nullable = false)
    private FeatureContext context;

    protected FeatureContextRelation() {}

    public FeatureContextRelation(Feature feature, FeatureContext context) {
        this.feature = feature;
        this.context = context;
        id = new FeatureContextRelationId(feature.getId(), context.getId());
    }

    public FeatureContextRelationId getId() {
        return id;
    }

    public Feature getFeature() {
        return feature;
    }

    public FeatureContext getContext() {
        return context;
    }
}
