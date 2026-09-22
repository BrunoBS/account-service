package br.com.itau.portalmanager.workspace.foundation.catalog.domain.visibility;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_visibilities")
public class VisibilityType extends BaseCatalogEntity {
}
