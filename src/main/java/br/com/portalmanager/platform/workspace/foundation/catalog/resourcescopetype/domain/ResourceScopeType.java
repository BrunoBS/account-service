package br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_resource_scopes")
public class ResourceScopeType extends CatalogEntity {
}
