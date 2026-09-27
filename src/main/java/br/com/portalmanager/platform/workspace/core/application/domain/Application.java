package br.com.portalmanager.platform.workspace.core.application.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applications", uniqueConstraints =
        @UniqueConstraint(name = "uk_applications_workspace_name", columnNames = {"workspace_id", "name"}))
public class Application {
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
    @Column(name = "application_scope_code", nullable = false, length = 50)
    private String applicationScope;
    @Column(name = "authorizer_group", length = 255)
    private String authorizerGroup;
    @Column(name = "settings", columnDefinition = "TEXT", nullable = false)
    private String settings;
    @Column(name = "is_default", nullable = false)
    private boolean defaultApplication;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Application() {}

    public Application(Long workspaceId, String name, String alias, String acronym, String applicationScope,
                       String authorizerGroup, String settings, boolean defaultApplication, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.workspaceId = workspaceId;
        update(name, alias, acronym, applicationScope, authorizerGroup, settings, defaultApplication, now);
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
    }

    public void update(String name, String alias, String acronym, String applicationScope,
                       String authorizerGroup, String settings, boolean defaultApplication, LocalDateTime now) {
        this.name = name;
        this.alias = alias;
        this.acronym = acronym;
        this.applicationScope = applicationScope;
        this.authorizerGroup = authorizerGroup;
        this.settings = settings;
        this.defaultApplication = defaultApplication;
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
    public String getApplicationScope() { return applicationScope; }
    public String getAuthorizerGroup() { return authorizerGroup; }
    public String getSettings() { return settings; }
    public boolean isDefaultApplication() { return defaultApplication; }
    public LifecycleTypeCode getLifecycle() { return lifecycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
