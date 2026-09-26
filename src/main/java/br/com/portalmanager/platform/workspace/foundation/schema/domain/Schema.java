package br.com.portalmanager.platform.workspace.foundation.schema.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "schema_definitions")
public class Schema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "schema_type_code", nullable = false, length = 50)
    private String schemaTypeCode;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "scope_code", nullable = false, length = 50))
    private SchemaScopeTypeCode scope;

    @Column(name = "workspace_id")
    private Long workspaceId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Schema() {
    }

    public Schema(
            String schemaTypeCode,
            SchemaScopeTypeCode scope,
            Long workspaceId,
            String code,
            String name,
            String description,
            LocalDateTime now
    ) {
        this.identifier = UUID.randomUUID().toString();
        this.schemaTypeCode = schemaTypeCode.trim();
        this.scope = scope;
        this.workspaceId = workspaceId;
        this.code = code;
        this.name = name.trim();
        this.description = description;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String description, LocalDateTime now) {
        this.name = name.trim();
        this.description = description;
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void quarantine(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.quarantined();
        updatedAt = now;
    }

    public boolean isActive() {
        return LifecycleTypeCode.active().equals(lifecycle);
    }

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getIdentifier() { return identifier; }
    public String getSchemaTypeCode() { return schemaTypeCode; }
    public SchemaScopeTypeCode getScope() { return scope; }
    public Long getWorkspaceId() { return workspaceId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LifecycleTypeCode getLifecycle() { return lifecycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
