package br.com.itau.portalmanager.workspace.foundation.catalog.domain.schemascope;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_schema_scopes")
public class SchemaScopeType extends BaseCatalogEntity {
}
