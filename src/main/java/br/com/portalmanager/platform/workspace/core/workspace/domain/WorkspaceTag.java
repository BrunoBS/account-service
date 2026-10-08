package br.com.portalmanager.platform.workspace.core.workspace.domain;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_tag")
public class WorkspaceTag extends Tag<Workspace> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace owner;

    protected WorkspaceTag() {
    }

    public WorkspaceTag(Workspace owner, TagName name, TagOriginType originType) {
        super(name, originType);
        this.owner = owner;
    }

    @Override
    public Workspace getOwner() {
        return owner;
    }
}
