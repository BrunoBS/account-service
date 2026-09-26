package br.com.portalmanager.platform.workspace.foundation.catalog.schematype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_schema_types")
public class SchemaType extends CatalogEntity {
}
