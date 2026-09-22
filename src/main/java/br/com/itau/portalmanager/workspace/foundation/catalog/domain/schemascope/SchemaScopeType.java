package br.com.itau.portalmanager.workspace.foundation.catalog.domain.schemascope;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_schema_scopes",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_schema_scopes_name", columnNames = "name")
)
public class SchemaScopeType extends BaseCatalogEntity {
}
