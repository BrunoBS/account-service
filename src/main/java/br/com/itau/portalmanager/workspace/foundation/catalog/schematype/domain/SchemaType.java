package br.com.itau.portalmanager.workspace.foundation.catalog.schematype.domain;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_schemas")
public class SchemaType extends BaseCatalogEntity {
}
