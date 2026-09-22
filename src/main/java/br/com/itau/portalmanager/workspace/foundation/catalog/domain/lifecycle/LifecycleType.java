package br.com.itau.portalmanager.workspace.foundation.catalog.domain.lifecycle;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_life_cycle",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_life_cycle_name", columnNames = "name")
)
public class LifecycleType extends BaseCatalogEntity {
}
