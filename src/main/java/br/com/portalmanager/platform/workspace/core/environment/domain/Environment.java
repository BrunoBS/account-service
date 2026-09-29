package br.com.portalmanager.platform.workspace.core.environment.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "environments")
public class Environment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "workspace_id", updatable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 250)
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "authorization_type_code", nullable = false, length = 50))
    private AuthorizationTypeCode authorizationType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_type_id", nullable = false)
    private EnvironmentType environmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_environment_id")
    private Environment parent;

    @Column(name = "authorizer_group", length = 255)
    private String authorizerGroup;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String settings;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Environment() {
    }

    public Environment(Long workspaceId, EnvironmentType environmentType, Environment parent,
                       String name, String description, AuthorizationTypeCode authorizationType,
                       String authorizerGroup, String settings, Integer sortOrder, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.workspaceId = workspaceId;
        this.environmentType = environmentType;
        this.parent = parent;
        update(name, description, authorizationType, authorizerGroup, settings, sortOrder, now);
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
    }

    public void update(String name, String description, AuthorizationTypeCode authorizationType, String authorizerGroup,
                       String settings, Integer sortOrder, LocalDateTime now) {
        this.name = name;
        this.description = description;
        this.authorizationType = authorizationType;
        this.authorizerGroup = authorizerGroup;
        this.settings = settings;
        this.sortOrder = sortOrder;
        this.updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void restore(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void quarantine(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.quarantined();
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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AuthorizationTypeCode getAuthorizationType() {
        return authorizationType;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public Environment getParent() { return parent; }
    public void changeType(EnvironmentType type, LocalDateTime now) {
        environmentType = type;
        updatedAt = now;
    }

    public String getAuthorizerGroup() {
        return authorizerGroup;
    }

    public String getSettings() {
        return settings;
    }

    public Integer getSortOrder() {
        return sortOrder;
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
