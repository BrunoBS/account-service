package br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class SchemaScopeTypeCode extends AbstractCatalogCode {

    protected SchemaScopeTypeCode() {}

    private SchemaScopeTypeCode(String value) {
        super(value);
    }

    public static SchemaScopeTypeCode of(String value) {
        return of(requireEnumValue(value, SchemaScopeTypeEnum.class));
    }

    public static SchemaScopeTypeCode of(SchemaScopeTypeEnum value) {
        return new SchemaScopeTypeCode(value == null ? null : value.name());
    }

    public static SchemaScopeTypeCode platform() {
        return of(SchemaScopeTypeEnum.PLATFORM);
    }

    public static SchemaScopeTypeCode workspace() {
        return of(SchemaScopeTypeEnum.WORKSPACE);
    }
}
