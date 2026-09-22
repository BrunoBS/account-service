package br.com.itau.portalmanager.workspace.foundation.catalog.applicationscope.domain;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_application_scopes")
public class ApplicationScopeType extends BaseCatalogEntity {
}
