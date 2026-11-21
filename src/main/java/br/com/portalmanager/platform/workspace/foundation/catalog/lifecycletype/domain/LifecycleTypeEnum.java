package br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain;

import br.com.portalmanager.platform.library.catalog.api.contract.model.CatalogEnum;

public enum LifecycleTypeEnum implements CatalogEnum<LifecycleTypeEnum> {
    ACTIVE,
    INACTIVE,
    QUARANTINED,
}
