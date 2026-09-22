package br.com.itau.portalmanager.workspace.foundation.catalog.sharestatus.domain;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_sharing_statuses")
public class ShareStatusType extends BaseCatalogEntity {
}
