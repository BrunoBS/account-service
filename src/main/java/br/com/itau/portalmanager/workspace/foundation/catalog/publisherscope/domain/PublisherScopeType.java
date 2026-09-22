package br.com.itau.portalmanager.workspace.foundation.catalog.publisherscope.domain;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_publisher_scopes")
public class PublisherScopeType extends BaseCatalogEntity {
}
