package br.com.itau.portalmanager.workspace.core.workspace.domain;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.LifecycleType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.WorkspaceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_type", nullable = false, length = 20)
    private WorkspaceType workspaceType;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "requester", nullable = false, length = 255)
    private String requester;

    @Column(name = "acronym", nullable = false, length = 5)
    private String acronym;

    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    @Column(name = "authorizer_group", length = 255)
    private String authorizerGroup;

    @Column(name = "email_group", nullable = false, length = 320)
    private String emailGroup;

    @Column(name = "onboarding", nullable = false)
    private boolean onboarding;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle", nullable = false, length = 20)
    private LifecycleType lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkspaceApprover> approvers = new LinkedHashSet<>();

    protected Workspace() {
    }

    public Workspace(
            WorkspaceType workspaceType,
            String name,
            String description,
            String requester,
            String acronym,
            String settings,
            String authorizerGroup,
            String emailGroup,
            LocalDateTime now) {
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
        this.lifecycle = LifecycleType.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            WorkspaceType workspaceType,
            String name,
            String description,
            String requester,
            String acronym,
            String settings,
            String authorizerGroup,
            String emailGroup,
            LocalDateTime now) {
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
        this.lifecycle = LifecycleType.INACTIVE;
        this.updatedAt = now;
    }

    public void restore(LocalDateTime now) {
        this.lifecycle = LifecycleType.ACTIVE;
        this.updatedAt = now;
    }

    public void updateDescription(String description, LocalDateTime updatedAt) {
        this.description = description;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public String getIdentifier() { return identifier; }
    public WorkspaceType getWorkspaceType() { return workspaceType; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRequester() { return requester; }
    public String getAcronym() { return acronym; }
    public String getSettings() { return settings; }
    public String getAuthorizerGroup() { return authorizerGroup; }
    public String getEmailGroup() { return emailGroup; }
    public boolean isOnboarding() { return onboarding; }
    public LifecycleType getLifecycle() { return lifecycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Set<WorkspaceApprover> getApprovers() {
        return Collections.unmodifiableSet(approvers);
    }
}
