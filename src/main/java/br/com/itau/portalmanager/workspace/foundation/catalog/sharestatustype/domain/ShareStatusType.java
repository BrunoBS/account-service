package br.com.itau.portalmanager.workspace.foundation.catalog.sharestatustype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_sharing_statuses")
public class ShareStatusType extends CatalogEntity {
}
