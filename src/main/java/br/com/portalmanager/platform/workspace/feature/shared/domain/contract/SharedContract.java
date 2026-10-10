package br.com.portalmanager.platform.workspace.feature.shared.domain.contract;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "shared_contracts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_shared_contracts_scope_feature",
        columnNames = { "owner_workspace_id", "owner_application_id", "feature_id" }
    )
)
public class SharedContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "owner_workspace_id", nullable = false)
    private Long ownerWorkspaceId;

    @Column(name = "owner_application_id", nullable = false)
    private Long ownerApplicationId;

    @Column(name = "feature_id", nullable = false, updatable = false)
    private Long featureId;

    @Column(length = 500)
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SharedContract() {}

    public SharedContract(Long workspaceId, Long applicationId, Long featureId, String description, LocalDateTime now) {
        identifier = UUID.randomUUID().toString();
        ownerWorkspaceId = workspaceId;
        ownerApplicationId = applicationId;
        this.featureId = java.util.Objects.requireNonNull(featureId);
        this.description = description;
        lifecycle = LifecycleTypeCode.active();
        createdAt = now;
        updatedAt = now;
    }

    public void update(String description, LocalDateTime now) {
        this.description = description;
        updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public boolean isActive() {
        return LifecycleTypeCode.active().equals(lifecycle);
    }

    public Long getFeatureId() {
        return featureId;
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

    public Long getOwnerWorkspaceId() {
        return ownerWorkspaceId;
    }

    public Long getOwnerApplicationId() {
        return ownerApplicationId;
    }

    public String getDescription() {
        return description;
    }

    public LifecycleTypeCode getLifecycle() {
        return lifecycle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
