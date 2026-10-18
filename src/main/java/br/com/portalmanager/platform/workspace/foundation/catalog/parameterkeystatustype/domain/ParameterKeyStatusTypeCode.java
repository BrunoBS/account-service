package br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;

import jakarta.persistence.Embeddable;

@Embeddable
public class ParameterKeyStatusTypeCode extends AbstractCatalogCode {
    protected ParameterKeyStatusTypeCode() {}

    private ParameterKeyStatusTypeCode(String value) {
        super(value);
    }

    public static ParameterKeyStatusTypeCode of(String value) {
        return of(requireEnumValue(value, ParameterKeyStatusTypeEnum.class));
    }

    public static ParameterKeyStatusTypeCode of(ParameterKeyStatusTypeEnum value) {
        return new ParameterKeyStatusTypeCode(value == null ? null : value.name());
    }

    public static ParameterKeyStatusTypeCode draft() {
        return of(ParameterKeyStatusTypeEnum.DRAFT);
    }

    public static ParameterKeyStatusTypeCode published() {
        return of(ParameterKeyStatusTypeEnum.PUBLISHED);
    }
}
