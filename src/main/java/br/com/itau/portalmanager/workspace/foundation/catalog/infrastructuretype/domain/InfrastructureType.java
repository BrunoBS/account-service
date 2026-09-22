package br.com.itau.portalmanager.workspace.foundation.catalog.infrastructuretype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_infrastructures")
public class InfrastructureType extends CatalogEntity {
}
