package br.com.itau.portalmanager.workspace.foundation.catalog.domain.language;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_languages",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_languages_name", columnNames = "name")
)
public class LanguageType extends BaseCatalogEntity {
}
