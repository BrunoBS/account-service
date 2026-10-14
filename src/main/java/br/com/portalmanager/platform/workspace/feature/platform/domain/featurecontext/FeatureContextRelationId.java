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
    private Long contextId;

    protected FeatureContextRelationId() {}

    public FeatureContextRelationId(Long featureId, Long contextId) {
        this.featureId = Objects.requireNonNull(featureId);
        this.contextId = Objects.requireNonNull(contextId);
    }

    public Long getFeatureId() {
        return featureId;
    }

    public Long getContextId() {
        return contextId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FeatureContextRelationId relationId)) return false;
        return (
            Objects.equals(featureId, relationId.featureId) &&
            Objects.equals(contextId, relationId.contextId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureId, contextId);
    }
}
