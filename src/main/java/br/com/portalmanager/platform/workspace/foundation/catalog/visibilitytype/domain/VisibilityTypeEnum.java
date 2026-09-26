package br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain;

import br.com.portalmanager.platform.library.catalog.model.CatalogEnum;

public enum VisibilityTypeEnum implements CatalogEnum<VisibilityTypeEnum> {
    PRIVATE,
    PUBLIC,
    INTERNAL;

    public static java.util.List<VisibilityTypeEnum> getAllowedVisibilities() {
        return java.util.List.of(PRIVATE, PUBLIC);
    }
}
