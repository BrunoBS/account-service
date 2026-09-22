package br.com.itau.portalmanager.workspace.foundation.catalog.domain.feature;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.featurescope.FeatureScopeType;
import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "type_features",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_type_features_scope_name",
                columnNames = {"type_feature_scopes_id", "name"}
        )
)
public class FeatureType extends BaseCatalogEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_feature_scopes_id", nullable = false)
    private FeatureScopeType featureScope;

    @Column(name = "available", nullable = false)
    private boolean available;

    public FeatureScopeType getFeatureScope() { return featureScope; }
    public void setFeatureScope(FeatureScopeType featureScope) { this.featureScope = featureScope; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
