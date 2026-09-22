package br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_features")
public class FeatureType extends CatalogEntity {
}
