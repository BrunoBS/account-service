package br.com.portalmanager.platform.workspace.core.workspace.domain;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.tagging.model.TagOwner;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "workspaces")
public class Workspace implements TagOwner {

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
    @AttributeOverride(name = "value", column = @Column(name = "workspace_type_code", nullable = false, length = 20))
    private WorkspaceTypeCode workspaceType;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "requester", nullable = false, length = 10)
    private String requester;

    @Column(name = "acronym", nullable = false, length = 5)
    private String acronym;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "JSON")
    private JsonNode settings;

    @AuthorizerGroup
    @Column(name = "authorizer_group", length = 10)
    private String authorizerGroup;

    @Column(name = "email_group", nullable = false, length = 100)
    private String emailGroup;

    @Column(name = "onboarding", nullable = false)
    private boolean onboarding;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 20))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkspaceApprover> approvers = new LinkedHashSet<>();

    protected Workspace() {}

    public Workspace(WorkspaceDetails details, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        applyDetails(details);
        this.onboarding = false;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(WorkspaceDetails details, LocalDateTime now) {
        applyDetails(details);
        this.updatedAt = now;
    }

    private void applyDetails(WorkspaceDetails details) {
        this.workspaceType = details.workspaceType();
        this.name = details.name();
        this.description = details.description();
        this.requester = details.requester();
        this.acronym = details.acronym();
        this.settings =
            details.settings() == null || details.settings().isNull() ? null : details.settings().deepCopy();
        this.authorizerGroup = details.authorizerGroup();
        this.emailGroup = details.emailGroup();
    }

    public void addApprover(String functional, String email) {
        approvers.add(new WorkspaceApprover(functional, email, this));
    }

    public void removeApprover(WorkspaceApprover approver) {
        approvers.remove(approver);
    }

    public void inactivate(LocalDateTime now) {
        transition(
            LifecycleTypeCode.active(),
            LifecycleTypeCode.inactive(),
            WorkspaceMessageKeys.INACTIVATE_INVALID,
            now
        );
    }

    public void restore(LocalDateTime now) {
        transition(LifecycleTypeCode.inactive(), LifecycleTypeCode.active(), WorkspaceMessageKeys.RESTORE_INVALID, now);
    }

    public void requireRestorable() {
        requireLifecycle(LifecycleTypeCode.inactive(), WorkspaceMessageKeys.RESTORE_INVALID);
    }

    public void requireDeletable() {
        requireLifecycle(LifecycleTypeCode.inactive(), WorkspaceMessageKeys.DELETE_INVALID);
    }

    public void quarantine(LocalDateTime now) {
        transition(
            LifecycleTypeCode.inactive(),
            LifecycleTypeCode.quarantined(),
            WorkspaceMessageKeys.DELETE_INVALID,
            now
        );
    }

    private void transition(
        LifecycleTypeCode expected,
        LifecycleTypeCode target,
        String messageKey,
        LocalDateTime now
    ) {
        requireLifecycle(expected, messageKey);
        lifecycle = target;
        updatedAt = now;
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
        return settings == null ? null : settings.deepCopy();
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

    public void completeOnboarding(LocalDateTime now) {
        if (onboarding) return;
        onboarding = true;
        updatedAt = now;
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
