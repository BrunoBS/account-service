package br.com.itau.portalmanager.workspace.foundation.catalog.domain.authorization;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum AuthorizationTypeEnum implements CatalogEnum<AuthorizationTypeEnum> {
    DEV(1),
    TST(2),
    ADM(3);

    private final int order;

    AuthorizationTypeEnum(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public static AuthorizationTypeEnum fromGroupName(String groupName) {
        if (groupName == null) return DEV;
        if (groupName.contains("-ADM_")) return ADM;
        if (groupName.contains("-TST_")) return TST;
        return DEV;
    }
}
