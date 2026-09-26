package br.com.itau.portalmanager.workspace.foundation.schema.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_schema_types")
public class SchemaType extends CatalogEntity {
}
