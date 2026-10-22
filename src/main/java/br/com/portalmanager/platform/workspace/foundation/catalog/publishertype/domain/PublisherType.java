package br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "publishers")
public class PublisherType extends CatalogEntity {}
