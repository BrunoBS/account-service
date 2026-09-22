package br.com.itau.portalmanager.workspace.foundation.catalog.domain.language;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_languages")
public class LanguageType extends BaseCatalogEntity {
}
