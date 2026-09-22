package br.com.itau.portalmanager.workspace.foundation.catalog.domain.tagorigin;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_tag_origins",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_tag_origins_name", columnNames = "name")
)
public class TagOriginType extends BaseCatalogEntity {
}
