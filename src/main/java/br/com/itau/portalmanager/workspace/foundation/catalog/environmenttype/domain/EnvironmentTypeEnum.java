package br.com.itau.portalmanager.workspace.foundation.catalog.environmenttype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum EnvironmentTypeEnum implements CatalogEnum<EnvironmentTypeEnum> {
    DEFAULT,
    CUSTOM;

    public static EnvironmentTypeEnum fromWorkspaceId(Long workspaceId) {
        return workspaceId == null ? DEFAULT : CUSTOM;
    }
}
