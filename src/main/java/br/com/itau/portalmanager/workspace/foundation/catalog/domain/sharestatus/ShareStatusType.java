package br.com.itau.portalmanager.workspace.foundation.catalog.domain.sharestatus;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_sharing_statuses",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_sharing_statuses_name", columnNames = "name")
)
public class ShareStatusType extends BaseCatalogEntity {
}
