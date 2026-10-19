package br.com.portalmanager.platform.workspace.core.environment.domain.compatibility;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "environment_type_compatibilities")
public class EnvironmentTypeCompatibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_type_code", referencedColumnName = "code", nullable = false)
    private EnvironmentType parentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_type_code", referencedColumnName = "code", nullable = false)
    private EnvironmentType childType;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EnvironmentTypeCompatibility() {}

    public EnvironmentTypeCompatibility(EnvironmentType parentType, EnvironmentType childType, LocalDateTime now) {
        identifier = UUID.randomUUID().toString();
        this.parentType = parentType;
        this.childType = childType;
        lifecycle = LifecycleTypeCode.active();
        createdAt = updatedAt = now;
    }

    public void restore(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.inactive());
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active());
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public String getIdentifier() {
        return identifier;
    }

    public EnvironmentType getParentType() {
        return parentType;
    }

    public EnvironmentType getChildType() {
        return childType;
    }

    public LifecycleTypeCode getLifecycle() {
        return lifecycle;
    }

    private void requireLifecycle(LifecycleTypeCode expected) {
        if (!expected.equals(lifecycle)) throw new ValidationException(EnvironmentMessageKeys.COMPATIBILITY_LIFECYCLE_INVALID);
    }
}
