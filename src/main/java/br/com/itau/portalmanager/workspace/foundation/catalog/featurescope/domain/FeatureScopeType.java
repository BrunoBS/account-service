package br.com.itau.portalmanager.workspace.foundation.catalog.featurescope.domain;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_feature_scopes")
public class FeatureScopeType extends BaseCatalogEntity {
}
