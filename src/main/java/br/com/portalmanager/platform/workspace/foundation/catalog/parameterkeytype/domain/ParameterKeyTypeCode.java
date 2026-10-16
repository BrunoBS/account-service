package br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;

import jakarta.persistence.Embeddable;

@Embeddable
public class ParameterKeyTypeCode extends AbstractCatalogCode {
    protected ParameterKeyTypeCode() {}

    private ParameterKeyTypeCode(String value) {
        super(value);
    }

    public static ParameterKeyTypeCode of(String value) {
        return of(requireEnumValue(value, ParameterKeyTypeEnum.class));
    }

    public static ParameterKeyTypeCode of(ParameterKeyTypeEnum value) {
        return new ParameterKeyTypeCode(value == null ? null : value.name());
    }

    public static ParameterKeyTypeCode configuration() {
        return of(ParameterKeyTypeEnum.CONFIGURATION);
    }

    public static ParameterKeyTypeCode expression() {
        return of(ParameterKeyTypeEnum.EXPRESSION);
    }
}
