package br.com.portalmanager.platform.workspace.core.workspace.domain;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    @Column(name = "identifier", nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "workspace_type_code", nullable = false, length = 50))
    private WorkspaceTypeCode workspaceType;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    @Column(name = "requester", nullable = false, length = 255)
    private String requester;
    @Column(name = "acronym", nullable = false, length = 5)
    private String acronym;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "json")
    private JsonNode settings;
    @AuthorizerGroup
    @Column(name = "authorizer_group", length = 255)
    private String authorizerGroup;
    @Column(name = "email_group", nullable = false, length = 320)
    private String emailGroup;
    @Column(name = "onboarding", nullable = false)
    private boolean onboarding;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL)
    private Set<WorkspaceApprover> approvers = new LinkedHashSet<>();

    protected Workspace() {
    }

    public Workspace(WorkspaceTypeCode workspaceType, String name, String description, String requester,
                     String acronym, JsonNode settings, String authorizerGroup, String emailGroup, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.workspaceType = workspaceType;
        this.name = name;
        this.description = description;
        this.requester = requester;
        this.acronym = acronym;
        this.settings = settings;
        this.authorizerGroup = authorizerGroup;
        this.emailGroup = emailGroup;
        this.onboarding = false;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(WorkspaceTypeCode workspaceType, String name, String description, String requester,
                       String acronym, JsonNode settings, String authorizerGroup, String emailGroup, LocalDateTime now) {
        this.workspaceType = workspaceType;
        this.name = name;
        this.description = description;
        this.requester = requester;
        this.acronym = acronym;
        this.settings = settings;
        this.authorizerGroup = authorizerGroup;
        this.emailGroup = emailGroup;
        this.updatedAt = now;
    }

    public void addApprover(String functional, String email) {
        approvers.add(new WorkspaceApprover(functional, email, this));
    }

    public void clearApprovers() {
        approvers.clear();
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

    public void updateDescription(String description, LocalDateTime updatedAt) {
        this.description = description;
        this.updatedAt = updatedAt;
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

    public WorkspaceTypeCode getWorkspaceType() {
        return workspaceType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getRequester() {
        return requester;
    }

    public String getAcronym() {
        return acronym;
    }

    public JsonNode getSettings() {
        return settings;
    }

    public String getAuthorizerGroup() {
        return authorizerGroup;
    }

    public String getEmailGroup() {
        return emailGroup;
    }

    public boolean isOnboarding() {
        return onboarding;
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

    public Set<WorkspaceApprover> getApprovers() {
        return Collections.unmodifiableSet(approvers);
    }
}
