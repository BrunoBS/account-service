package br.com.portalmanager.platform.workspace.core.workspace.domain;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.foundation.tagging.domain.AbstractTagEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_tag")
public class WorkspaceTag extends AbstractTagEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    protected WorkspaceTag() {
    }

    public WorkspaceTag(Workspace workspace, String name, TagOriginType originType) {
        super(name, originType);
        this.workspace = workspace;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
