package br.com.itau.portalmanager.workspace.foundation.catalog.applicationscopetype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_application_scopes")
public class ApplicationScopeType extends CatalogEntity {
}
