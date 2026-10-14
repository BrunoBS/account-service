package br.com.portalmanager.platform.workspace.core.environment.domain.environment;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

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

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 250)
    private String description;

    @Embedded
    @AttributeOverride(
        name = "value",
        column = @Column(name = "authorization_type_code", nullable = false, length = 50)
    )
    private AuthorizationTypeCode authorizationType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_type_code", referencedColumnName = "code", nullable = false)
    private EnvironmentType environmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_environment_id", updatable = false)
    private Environment parent;

    @AuthorizerGroup
    @Column(name = "authorizer_group", length = 255)
    private String authorizerGroup;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private JsonNode settings;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Environment() {}

    public Environment(
        Long workspaceId,
        EnvironmentType environmentType,
        Environment parent,
        EnvironmentDetails details,
        LocalDateTime now
    ) {
        this.identifier = UUID.randomUUID().toString();
        this.workspaceId = workspaceId;
        this.environmentType = environmentType;
        this.parent = parent;
        applyDetails(details, now);
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
    }

    public void update(EnvironmentDetails details, LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active(), EnvironmentMessageKeys.NOT_FOUND);
        applyDetails(details, now);
    }

    private void applyDetails(EnvironmentDetails details, LocalDateTime now) {
        this.name = details.name();
        this.description = details.description();
        this.authorizationType = details.authorizationType();
        this.authorizerGroup = details.authorizerGroup();
        this.settings = details.settings() == null || details.settings().isNull() ? null : details.settings().deepCopy();
        this.sortOrder = details.sortOrder();
        this.updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active(), EnvironmentMessageKeys.NOT_FOUND);
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void restore(LocalDateTime now) {
        requireRestorable();
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void quarantine(LocalDateTime now) {
        requireDeletable();
        lifecycle = LifecycleTypeCode.quarantined();
        updatedAt = now;
    }

    public void requireRestorable() {
        requireLifecycle(LifecycleTypeCode.inactive(), EnvironmentMessageKeys.RESTORE_INVALID);
    }

    public void requireDeletable() {
        requireLifecycle(LifecycleTypeCode.inactive(), EnvironmentMessageKeys.DELETE_INVALID);
    }

    private void requireLifecycle(LifecycleTypeCode expected, String messageKey) {
        if (!expected.equals(lifecycle)) throw new ValidationException(messageKey);
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

    public Environment getParent() {
        return parent;
    }

    public void changeType(EnvironmentType type, LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active(), EnvironmentMessageKeys.NOT_FOUND);
        environmentType = type;
        updatedAt = now;
    }

    public String getAuthorizerGroup() {
        return authorizerGroup;
    }

    public JsonNode getSettings() {
        return settings == null ? null : settings.deepCopy();
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
