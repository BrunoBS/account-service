package br.com.itau.portalmanager.workspace.foundation.catalog.domain.visibility;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_visibilities",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_visibilities_name", columnNames = "name")
)
public class VisibilityType extends BaseCatalogEntity {
}
