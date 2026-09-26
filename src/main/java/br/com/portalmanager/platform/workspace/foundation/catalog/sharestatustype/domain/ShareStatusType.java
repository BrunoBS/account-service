package br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_sharing_statuses")
public class ShareStatusType extends CatalogEntity {
}
