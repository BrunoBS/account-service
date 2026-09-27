package br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class EnvironmentTypeCode extends AbstractCatalogCode {
    protected EnvironmentTypeCode() {}

    private EnvironmentTypeCode(EnvironmentTypeEnum value) { super(value); }

    public static EnvironmentTypeCode fromWorkspaceId(Long workspaceId) {
        return new EnvironmentTypeCode(EnvironmentTypeEnum.fromWorkspaceId(workspaceId));
    }
}
