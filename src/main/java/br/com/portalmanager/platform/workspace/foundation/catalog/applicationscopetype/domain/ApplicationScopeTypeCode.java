package br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class ApplicationScopeTypeCode extends AbstractCatalogCode {

    protected ApplicationScopeTypeCode() {}

    private ApplicationScopeTypeCode(String value) {
        super(value);
    }

    public static ApplicationScopeTypeCode of(String value) {
        return of(requireEnumValue(value, ApplicationScopeTypeEnum.class));
    }

    public static ApplicationScopeTypeCode of(ApplicationScopeTypeEnum value) {
        return new ApplicationScopeTypeCode(value == null ? null : value.name());
    }
}
