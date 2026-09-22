package br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_infrastructures",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_infrastructures_name", columnNames = "name")
)
public class InfrastructureType extends BaseCatalogEntity {
}
