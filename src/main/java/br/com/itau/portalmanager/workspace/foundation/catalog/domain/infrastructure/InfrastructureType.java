package br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_infrastructures")
public class InfrastructureType extends BaseCatalogEntity {
}
