package br.com.portalmanager.platform.workspace.foundation.catalog.sharestatustype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShareStatusTypeCode extends AbstractCatalogCode {

    protected ShareStatusTypeCode() {}

    private ShareStatusTypeCode(String value) {
        super(value);
    }

    public static ShareStatusTypeCode of(String value) {
        return of(requireEnumValue(value, ShareStatusTypeEnum.class));
    }

    public static ShareStatusTypeCode of(ShareStatusTypeEnum value) {
        return new ShareStatusTypeCode(value == null ? null : value.name());
    }

    public static ShareStatusTypeCode pending() { return of(ShareStatusTypeEnum.PENDING); }

    public static ShareStatusTypeCode approved() { return of(ShareStatusTypeEnum.APPROVED); }

    public static ShareStatusTypeCode rejected() { return of(ShareStatusTypeEnum.REJECTED); }

    public static ShareStatusTypeCode revoked() { return of(ShareStatusTypeEnum.REVOKED); }
}
