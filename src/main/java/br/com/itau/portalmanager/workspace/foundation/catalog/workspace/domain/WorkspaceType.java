package br.com.itau.portalmanager.workspace.foundation.catalog.workspace.domain;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_workspaces")
public class WorkspaceType extends BaseCatalogEntity {
}
