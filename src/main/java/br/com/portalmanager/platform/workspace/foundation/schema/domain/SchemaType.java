package br.com.portalmanager.platform.workspace.foundation.schema.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "schema_types")
public class SchemaType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "scope_code", nullable = false, length = 50))
    private SchemaScopeTypeCode scope;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SchemaType() {
    }

    public SchemaType(
            String code,
            String name,
            String description,
            SchemaScopeTypeCode scope,
            LocalDateTime now
    ) {
        this.identifier = UUID.randomUUID().toString();
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.description = description;
        this.scope = scope;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String name,
            String description,
            SchemaScopeTypeCode scope,
            LocalDateTime now
    ) {
        this.name = name.trim();
        this.description = description;
        this.scope = scope;
        this.updatedAt = now;
    }

    public boolean allowsScope(SchemaScopeTypeCode scope) {
        return Objects.equals(getScope(), scope);
    }

    public boolean isActive() {
        return LifecycleTypeCode.active().equals(lifecycle);
    }

    public void activate(LocalDateTime now) {
        this.lifecycle = LifecycleTypeCode.active();
        this.updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        this.lifecycle = LifecycleTypeCode.inactive();
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getIdentifier() { return identifier; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LifecycleTypeCode getLifecycle() { return lifecycle; }
    public SchemaScopeTypeCode getScope() { return scope; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
