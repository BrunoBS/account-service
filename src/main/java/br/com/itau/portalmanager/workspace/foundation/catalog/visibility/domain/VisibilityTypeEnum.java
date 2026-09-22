package br.com.itau.portalmanager.workspace.foundation.catalog.visibility.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum VisibilityTypeEnum implements CatalogEnum<VisibilityTypeEnum> {
    PRIVATE,
    PUBLIC,
    INTERNAL;

    public static java.util.List<VisibilityTypeEnum> getAllowedVisibilities() {
        return java.util.List.of(PRIVATE, PUBLIC);
    }
}
