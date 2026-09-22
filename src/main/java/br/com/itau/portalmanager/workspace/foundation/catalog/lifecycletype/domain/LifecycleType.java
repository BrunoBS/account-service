package br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_life_cycle")
public class LifecycleType extends CatalogEntity {
}
