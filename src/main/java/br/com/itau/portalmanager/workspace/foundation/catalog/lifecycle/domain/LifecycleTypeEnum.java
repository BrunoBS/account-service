package br.com.itau.portalmanager.workspace.foundation.catalog.lifecycle.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum LifecycleTypeEnum implements CatalogEnum<LifecycleTypeEnum> {
    ACTIVE,
    INACTIVE,
    PENDING_DELETION
}
