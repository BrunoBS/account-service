package br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_visibilities")
public class VisibilityType extends CatalogEntity {
}
