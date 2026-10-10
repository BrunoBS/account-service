package br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class AuthorizationTypeCode extends AbstractCatalogCode {

    protected AuthorizationTypeCode() {}

    private AuthorizationTypeCode(String value) {
        super(value);
    }

    public static AuthorizationTypeCode of(String value) {
        return of(requireEnumValue(value, AuthorizationTypeEnum.class));
    }

    public static AuthorizationTypeCode of(AuthorizationTypeEnum value) {
        return new AuthorizationTypeCode(value == null ? null : value.name());
    }
}
