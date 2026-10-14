package br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class VisibilityTypeCode extends AbstractCatalogCode {

    protected VisibilityTypeCode() {}

    private VisibilityTypeCode(String value) {
        super(value);
    }

    public static VisibilityTypeCode of(String value) {
        return of(requireEnumValue(value, VisibilityTypeEnum.class));
    }

    public static VisibilityTypeCode of(VisibilityTypeEnum value) {
        return new VisibilityTypeCode(value == null ? null : value.name());
    }

    public static VisibilityTypeCode privateVisibility() { return of(VisibilityTypeEnum.PRIVATE); }

    public static VisibilityTypeCode publicVisibility() { return of(VisibilityTypeEnum.PUBLIC); }

    public static VisibilityTypeCode internal() { return of(VisibilityTypeEnum.INTERNAL); }
}
