package br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum InfrastructureTypeEnum implements CatalogEnum<InfrastructureTypeEnum> {
    VM,
    CONTAINER,
    KUBERNETES,
    SERVERLESS
}
