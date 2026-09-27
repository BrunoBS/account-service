package br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class ResourceScopeTypeCode extends AbstractCatalogCode {
    protected ResourceScopeTypeCode() {}

    private ResourceScopeTypeCode(String value) { super(value); }

    public static ResourceScopeTypeCode of(String value) { return new ResourceScopeTypeCode(value); }
}
