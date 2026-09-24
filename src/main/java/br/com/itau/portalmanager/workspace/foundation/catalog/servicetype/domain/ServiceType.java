package br.com.itau.portalmanager.workspace.foundation.catalog.servicetype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_services")
public class ServiceType extends CatalogEntity {
}
