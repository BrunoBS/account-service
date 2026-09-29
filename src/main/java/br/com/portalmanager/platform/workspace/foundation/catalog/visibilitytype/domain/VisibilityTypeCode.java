package br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class VisibilityTypeCode extends AbstractCatalogCode {
    protected VisibilityTypeCode() {}

    private VisibilityTypeCode(String value) { super(value); }

    public static VisibilityTypeCode of(String value) { return new VisibilityTypeCode(value); }

    public static VisibilityTypeCode of(VisibilityTypeEnum value) { return of(value.name()); }
}
