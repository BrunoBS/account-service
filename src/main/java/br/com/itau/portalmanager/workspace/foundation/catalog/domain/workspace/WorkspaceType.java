package br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_workspaces")
public class WorkspaceType extends BaseCatalogEntity {
}
