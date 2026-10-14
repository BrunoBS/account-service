package br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "environment_types")
public class EnvironmentType {

    @Version
    private Long version;

    @Id
    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 250)
    private String description;

    @Column(name = "root_allowed", nullable = false)
    private boolean rootAllowed;

    @Column(name = "allows_children", nullable = false)
    private boolean allowsChildren;

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
        EnvironmentTypeDetails details,
        LocalDateTime now
    ) {
        this.code = code;
        applyDetails(details);
        updatedAt = now;
        lifecycle = LifecycleTypeCode.active();
        createdAt = now;
    }

    public void update(EnvironmentTypeDetails details, LocalDateTime now) {
        requireActiveForUpdate();
        applyDetails(details);
        updatedAt = now;
    }

    private void applyDetails(EnvironmentTypeDetails details) {
        name = details.name();
        description = details.description();
        rootAllowed = details.rootAllowed();
        allowsChildren = details.allowsChildren();
        workspaceRequired = details.workspaceRequired();
        displayOrder = details.displayOrder();
    }

    public void inactivate(LocalDateTime now) {
        requireActiveForInactivation();
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void restore(LocalDateTime now) {
        if (!LifecycleTypeCode.inactive().equals(lifecycle)) throw new ValidationException(
            EnvironmentMessageKeys.TYPE_RESTORE_INVALID
        );
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public Long getVersion() {
        return version;
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

    public boolean allowsChildren() {
        return allowsChildren;
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

    public void requireActiveForUpdate() {
        if (!LifecycleTypeCode.active().equals(lifecycle)) throw new ValidationException(
            EnvironmentMessageKeys.TYPE_UPDATE_INVALID
        );
    }

    public void requireActiveForInactivation() {
        if (!LifecycleTypeCode.active().equals(lifecycle)) throw new ValidationException(
            EnvironmentMessageKeys.TYPE_INACTIVATE_INVALID
        );
    }
}
