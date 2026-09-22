package br.com.itau.portalmanager.workspace.foundation.catalog.domain.environment;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_environments",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_environments_name", columnNames = "name")
)
public class EnvironmentType extends BaseCatalogEntity {
}
