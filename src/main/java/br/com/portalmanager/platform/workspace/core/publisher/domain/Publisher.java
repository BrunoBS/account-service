package br.com.portalmanager.platform.workspace.core.publisher.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "publishers")
public class Publisher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false)
    private Long version;
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String identifier;
    @Column(nullable = false, unique = true, updatable = false, length = 40)
    private String code;
    @Column(nullable = false, length = 50)
    private String name;
    @Column(nullable = false, length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "publisher_scope", nullable = false, length = 20)
    private ResourceScopeTypeEnum scope;
    @Column(nullable = false)
    private boolean deprecated;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Publisher() {}
    public Publisher(String code, String name, String description, ResourceScopeTypeEnum scope, boolean deprecated, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.code = code;
        update(name, description, scope, deprecated, now);
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
    }
    public void update(String name, String description, ResourceScopeTypeEnum scope, boolean deprecated, LocalDateTime now) {
        this.name = name;
        this.description = description;
        this.scope = scope;
        this.deprecated = deprecated;
        this.updatedAt = now;
    }
    public void inactivate(LocalDateTime now) { lifecycle = LifecycleTypeCode.inactive(); updatedAt = now; }
    public void restore(LocalDateTime now) { lifecycle = LifecycleTypeCode.active(); updatedAt = now; }
    public void quarantine(LocalDateTime now) { lifecycle = LifecycleTypeCode.quarantined(); updatedAt = now; }
    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getIdentifier() { return identifier; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ResourceScopeTypeEnum getScope() { return scope; }
    public boolean isDeprecated() { return deprecated; }
    public LifecycleTypeCode getLifecycle() { return lifecycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
