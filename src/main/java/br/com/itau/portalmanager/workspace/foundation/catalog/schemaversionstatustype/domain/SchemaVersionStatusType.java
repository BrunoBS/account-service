package br.com.itau.portalmanager.workspace.foundation.catalog.schemaversionstatustype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_schema_version_status")
public class SchemaVersionStatusType extends CatalogEntity {
}
