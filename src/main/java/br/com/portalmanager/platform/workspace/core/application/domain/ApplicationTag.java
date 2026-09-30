package br.com.portalmanager.platform.workspace.core.application.domain;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.foundation.tagging.domain.AbstractTagEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_tag")
public class ApplicationTag extends AbstractTagEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    protected ApplicationTag() {
    }

    public ApplicationTag(Application application, String name, TagOriginType originType) {
        super(name, originType);
        this.application = application;
    }

    public Application getApplication() {
        return application;
    }
}
