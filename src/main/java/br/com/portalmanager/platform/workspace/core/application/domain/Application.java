package br.com.portalmanager.platform.workspace.core.application.domain;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applications", uniqueConstraints =
        @UniqueConstraint(name = "uk_applications_workspace_name", columnNames = {"workspace_id", "name"}))
public class Application implements TagOwner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false)
    private Long version;
    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private Long workspaceId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 100)
    private String alias;
    @Column(nullable = false, length = 20)
    private String acronym;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "application_scope_code", nullable = false, length = 50))
    private ApplicationScopeTypeCode applicationScope;
    @AuthorizerGroup
    @Column(name = "authorizer_group", length = 255)
    private String authorizerGroup;
    @Column(name = "settings", columnDefinition = "TEXT", nullable = false)
    private String settings;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Application() {}

    public Application(Long workspaceId, String name, String alias, String acronym, ApplicationScopeTypeCode applicationScope,
                       String authorizerGroup, String settings, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.workspaceId = workspaceId;
        update(name, alias, acronym, applicationScope, authorizerGroup, settings, now);
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
    }

    public void update(String name, String alias, String acronym, ApplicationScopeTypeCode applicationScope,
                       String authorizerGroup, String settings, LocalDateTime now) {
        this.name = name;
        this.alias = alias;
        this.acronym = acronym;
        this.applicationScope = applicationScope;
        this.authorizerGroup = authorizerGroup;
        this.settings = settings;
        this.updatedAt = now;
    }

    public void inactivate(LocalDateTime now) { lifecycle = LifecycleTypeCode.inactive(); updatedAt = now; }
    public void restore(LocalDateTime now) { lifecycle = LifecycleTypeCode.active(); updatedAt = now; }
    public void quarantine(LocalDateTime now) { lifecycle = LifecycleTypeCode.quarantined(); updatedAt = now; }

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getIdentifier() { return identifier; }
    public Long getWorkspaceId() { return workspaceId; }
    public String getName() { return name; }
    public String getAlias() { return alias; }
    public String getAcronym() { return acronym; }
    public ApplicationScopeTypeCode getApplicationScope() { return applicationScope; }
    public String getAuthorizerGroup() { return authorizerGroup; }
    public String getSettings() { return settings; }
    public LifecycleTypeCode getLifecycle() { return lifecycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
