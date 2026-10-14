package br.com.portalmanager.platform.workspace.core.application.domain;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_applications_workspace_name",
        columnNames = { "workspace_id", "name" }
    )
)
public class Application implements TagOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String alias;

    @Column(length = 200)
    private String description;

    @Column(nullable = false, length = 5)
    private String acronym;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "application_scope_code", nullable = false, length = 20))
    private ApplicationScopeTypeCode applicationScope;

    @AuthorizerGroup
    @Column(name = "authorizer_group", length = 10)
    private String authorizerGroup;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false, columnDefinition = "JSON")
    private JsonNode settings;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 20))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Application() {}

    public Application(Long workspaceId, ApplicationDetails details, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.workspaceId = workspaceId;
        applyDetails(details);
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(ApplicationDetails details, LocalDateTime now) {
        applyDetails(details);
        updatedAt = now;
    }

    private void applyDetails(ApplicationDetails details) {
        name = details.name();
        alias = details.alias();
        description = details.description();
        acronym = details.acronym();
        applicationScope = details.applicationScope();
        authorizerGroup = details.authorizerGroup();
        settings = details.settings() == null || details.settings().isNull() ? null : details.settings().deepCopy();
    }

    public void requireRestorable() {
        requireLifecycle(LifecycleTypeCode.inactive(), ApplicationMessageKeys.RESTORE_INVALID);
    }

    public void requireDeletable() {
        requireLifecycle(LifecycleTypeCode.inactive(), ApplicationMessageKeys.DELETE_INVALID);
    }

    private void requireLifecycle(LifecycleTypeCode expected, String messageKey) {
        if (!expected.equals(lifecycle)) throw new ValidationException(messageKey);
    }

    public void inactivate(LocalDateTime now) {
        requireLifecycle(LifecycleTypeCode.active(), ApplicationMessageKeys.NOT_FOUND);
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

    public String getAlias() {
        return alias;
    }

    public String getAcronym() {
        return acronym;
    }

    public ApplicationScopeTypeCode getApplicationScope() {
        return applicationScope;
    }

    public String getAuthorizerGroup() {
        return authorizerGroup;
    }

    public JsonNode getSettings() {
        return settings == null ? null : settings.deepCopy();
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
