package br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class LifecycleTypeCode extends AbstractCatalogCode {

    protected LifecycleTypeCode() {
    }

    private LifecycleTypeCode(String value) {
        super(value);
    }

    public static LifecycleTypeCode of(String value) {
        return new LifecycleTypeCode(value);
    }

    public static LifecycleTypeCode of(LifecycleTypeEnum value) {
        return of(value.name());
    }

    public static LifecycleTypeCode active() {
        return of(LifecycleTypeEnum.ACTIVE);
    }

    public static LifecycleTypeCode inactive() {
        return of(LifecycleTypeEnum.INACTIVE);
    }

    public static LifecycleTypeCode quarantined() {
        return of(LifecycleTypeEnum.QUARANTINED);
    }
}
