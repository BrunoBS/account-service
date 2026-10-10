package br.com.portalmanager.platform.workspace.feature.shared.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "shared_contracts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_shared_contracts_scope_name",
        columnNames = { "owner_workspace_identifier", "owner_application_identifier", "name" }
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

    @Column(name = "owner_workspace_identifier", nullable = false, length = 36)
    private String ownerWorkspaceIdentifier;

    @Column(name = "owner_application_identifier", nullable = false, length = 36)
    private String ownerApplicationIdentifier;

    @Column(nullable = false, length = 120)
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

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SharedParticipation> participations = new LinkedHashSet<>();

    protected SharedContract() {}

    public SharedContract(
        String workspaceIdentifier,
        String applicationIdentifier,
        String name,
        String description,
        LocalDateTime now
    ) {
        identifier = UUID.randomUUID().toString();
        ownerWorkspaceIdentifier = workspaceIdentifier;
        ownerApplicationIdentifier = applicationIdentifier;
        this.name = name;
        this.description = description;
        lifecycle = LifecycleTypeCode.active();
        createdAt = now;
        updatedAt = now;
    }

    public void update(String name, String description, LocalDateTime now) {
        this.name = name;
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

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getOwnerWorkspaceIdentifier() {
        return ownerWorkspaceIdentifier;
    }

    public String getOwnerApplicationIdentifier() {
        return ownerApplicationIdentifier;
    }

    public String getName() {
        return name;
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

    public Set<SharedParticipation> getParticipations() {
        return participations;
    }
}
