package br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum InfrastructureTypeEnum implements CatalogEnum<InfrastructureTypeEnum> {
    VM,
    CONTAINER,
    KUBERNETES,
    SERVERLESS
}
