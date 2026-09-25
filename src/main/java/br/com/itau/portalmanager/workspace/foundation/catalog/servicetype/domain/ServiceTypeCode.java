package br.com.itau.portalmanager.workspace.foundation.catalog.servicetype.domain;

import br.com.portalmanager.platform.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class ServiceTypeCode extends AbstractCatalogCode {

    protected ServiceTypeCode() {
    }

    private ServiceTypeCode(String value) {
        super(value);
    }

    public static ServiceTypeCode of(String value) {
        return new ServiceTypeCode(value);
    }
}
