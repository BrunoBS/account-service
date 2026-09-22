package br.com.itau.portalmanager.workspace.foundation.catalog.domain.schematype;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.schemascope.SchemaScopeType;
import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "type_schemas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_type_schemas_scope_name",
                columnNames = {"type_schema_scopes_id", "name"}
        )
)
public class SchemaType extends BaseCatalogEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_schema_scopes_id", nullable = false)
    private SchemaScopeType schemaScope;

    public SchemaScopeType getSchemaScope() { return schemaScope; }
    public void setSchemaScope(SchemaScopeType schemaScope) { this.schemaScope = schemaScope; }
}
