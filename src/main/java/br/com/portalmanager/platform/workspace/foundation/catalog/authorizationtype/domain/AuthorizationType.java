package br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain;

import br.com.portalmanager.platform.library.catalog.api.contract.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_authorizations")
public class AuthorizationType extends CatalogEntity {}
