package br.com.itau.portalmanager.workspace.foundation.catalog.languagetype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_languages")
public class LanguageType extends CatalogEntity {
}
