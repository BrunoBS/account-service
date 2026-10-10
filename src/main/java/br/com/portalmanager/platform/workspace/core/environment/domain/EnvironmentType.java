package br.com.portalmanager.platform.workspace.core.environment.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "environment_types")
public class EnvironmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String identifier;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 250)
    private String description;

    @Column(name = "root_allowed", nullable = false)
    private boolean rootAllowed;

    @Column(name = "workspace_required", nullable = false)
    private boolean workspaceRequired;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EnvironmentType() {}

    public EnvironmentType(
        String code,
        String name,
        String description,
        boolean rootAllowed,
        boolean workspaceRequired,
        int displayOrder,
        LocalDateTime now
    ) {
        identifier = UUID.randomUUID().toString();
        this.code = code;
        update(name, description, rootAllowed, workspaceRequired, displayOrder, now);
        lifecycle = LifecycleTypeCode.active();
        createdAt = now;
    }

    public void update(
        String name,
        String description,
        boolean rootAllowed,
        boolean workspaceRequired,
        int displayOrder,
        LocalDateTime now
    ) {
        this.name = name;
        this.description = description;
        this.rootAllowed = rootAllowed;
        this.workspaceRequired = workspaceRequired;
        this.displayOrder = displayOrder;
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void restore(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRootAllowed() {
        return rootAllowed;
    }

    public boolean isWorkspaceRequired() {
        return workspaceRequired;
    }

    public LifecycleTypeCode getLifecycle() {
        return lifecycle;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
