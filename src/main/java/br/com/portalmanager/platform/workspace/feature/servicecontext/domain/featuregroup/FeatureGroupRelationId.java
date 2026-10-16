package br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FeatureGroupRelationId implements Serializable {

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Column(name = "feature_group_id", nullable = false)
    private Long featureGroupId;

    protected FeatureGroupRelationId() {}

    public FeatureGroupRelationId(Long featureId, Long featureGroupId) {
        this.featureId = Objects.requireNonNull(featureId);
        this.featureGroupId = Objects.requireNonNull(featureGroupId);
    }

    public Long getFeatureId() {
        return featureId;
    }

    public Long getFeatureGroupId() {
        return featureGroupId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FeatureGroupRelationId relationId)) return false;
        return (Objects.equals(featureId, relationId.featureId)
                && Objects.equals(featureGroupId, relationId.featureGroupId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureId, featureGroupId);
    }
}
