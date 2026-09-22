package br.com.itau.portalmanager.workspace.foundation.catalog.infrastructure.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum InfrastructureTypeEnum implements CatalogEnum<InfrastructureTypeEnum> {
    VM,
    CONTAINER,
    KUBERNETES,
    SERVERLESS
}
