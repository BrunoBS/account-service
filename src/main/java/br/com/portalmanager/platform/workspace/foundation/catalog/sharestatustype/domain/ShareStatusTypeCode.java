package br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShareStatusTypeCode extends AbstractCatalogCode {
    protected ShareStatusTypeCode() {}

    private ShareStatusTypeCode(String value) { super(value); }

    public static ShareStatusTypeCode of(String value) { return new ShareStatusTypeCode(value); }

    public static ShareStatusTypeCode of(ShareStatusTypeEnum value) { return of(value.name()); }
}
