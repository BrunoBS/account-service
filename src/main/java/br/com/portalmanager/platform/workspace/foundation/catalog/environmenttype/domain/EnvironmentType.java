package br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_environments")
public class EnvironmentType extends CatalogEntity {
}
