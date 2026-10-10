package br.com.portalmanager.platform.workspace.core.workspace.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "workspace_approvers")
public class WorkspaceApprover {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "functional", nullable = false, length = 255)
    private String functional;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    protected WorkspaceApprover() {}

    WorkspaceApprover(String functional, String email, Workspace workspace) {
        this.id = UUID.randomUUID().toString();
        this.functional = functional;
        this.email = email;
        this.workspace = workspace;
    }

    public String getId() {
        return id;
    }

    public String getFunctional() {
        return functional;
    }

    public String getEmail() {
        return email;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
