package br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_schema_version_status")
public class SchemaVersionStatusType extends CatalogEntity {
}
