package br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_tag_origins")
public class TagOriginType extends CatalogEntity {}
