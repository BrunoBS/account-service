package br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FeatureContextRelationId implements Serializable {

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Column(name = "feature_context_id", nullable = false)
    private Long featureContextId;

    protected FeatureContextRelationId() {}

    public FeatureContextRelationId(Long featureId, Long featureContextId) {
        this.featureId = Objects.requireNonNull(featureId);
        this.featureContextId = Objects.requireNonNull(featureContextId);
    }

    public Long getFeatureId() {
        return featureId;
    }

    public Long getFeatureContextId() {
        return featureContextId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FeatureContextRelationId relationId)) return false;
        return (
            Objects.equals(featureId, relationId.featureId) &&
            Objects.equals(featureContextId, relationId.featureContextId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureId, featureContextId);
    }
}
