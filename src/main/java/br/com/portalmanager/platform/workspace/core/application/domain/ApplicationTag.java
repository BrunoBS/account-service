package br.com.portalmanager.platform.workspace.core.application.domain;

import br.com.portalmanager.platform.library.tagging.model.Tag;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_tag")
public class ApplicationTag extends Tag<Application> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application owner;

    protected ApplicationTag() {}

    public ApplicationTag(Application owner, TagName name, TagOriginType originType) {
        super(name, originType);
        this.owner = owner;
    }

    @Override
    public Application getOwner() {
        return owner;
    }
}
