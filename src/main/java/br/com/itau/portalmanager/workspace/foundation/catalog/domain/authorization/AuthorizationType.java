package br.com.itau.portalmanager.workspace.foundation.catalog.domain.authorization;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "type_authorizations",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_authorizations_name", columnNames = "name")
)
public class AuthorizationType extends BaseCatalogEntity {
}
