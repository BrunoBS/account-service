package br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_workspaces",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_workspaces_name", columnNames = "name")
)
public class WorkspaceType extends BaseCatalogEntity {
}
