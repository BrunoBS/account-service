package br.com.itau.portalmanager.workspace.foundation.catalog.environmenttype.domain;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_environments")
public class EnvironmentType extends BaseCatalogEntity {
}
