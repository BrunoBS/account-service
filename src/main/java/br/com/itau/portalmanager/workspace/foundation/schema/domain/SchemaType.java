package br.com.itau.portalmanager.workspace.foundation.schema.domain;

import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "schema_types")
public class SchemaType {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private Long version;
    @Column(nullable = false, unique = true, length = 36, updatable = false) private String identifier;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, unique = true, length = 100) private String name;
    @Column(length = 500) private String description;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected SchemaType() {
    }

    public SchemaType(String code, String name, String description, LocalDateTime now) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Schema type code must use lowercase kebab-case");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Schema type name is required");
        }
        this.identifier = UUID.randomUUID().toString();
        this.code = code;
        this.name = name.trim();
        this.description = description;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String description, LocalDateTime now) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Schema type name is required");
        }
        this.name = name.trim();
        this.description = description;
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) { lifecycle = LifecycleTypeCode.active(); updatedAt = now; }
    public void inactivate(LocalDateTime now) { lifecycle = LifecycleTypeCode.inactive(); updatedAt = now; }
    public void quarantine(LocalDateTime now) { lifecycle = LifecycleTypeCode.quarantined(); updatedAt = now; }
    public boolean isActive() { return LifecycleTypeCode.active().equals(lifecycle); }

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getIdentifier() { return identifier; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LifecycleTypeCode getLifecycle() { return lifecycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
