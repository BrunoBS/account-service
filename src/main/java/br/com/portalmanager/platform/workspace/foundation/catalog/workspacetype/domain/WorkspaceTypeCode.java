package br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class WorkspaceTypeCode extends AbstractCatalogCode {

    protected WorkspaceTypeCode() {}

    private WorkspaceTypeCode(String value) {
        super(value);
    }

    public static WorkspaceTypeCode of(String value) {
        return of(requireEnumValue(value, WorkspaceTypeEnum.class));
    }

    public static WorkspaceTypeCode of(WorkspaceTypeEnum value) {
        return new WorkspaceTypeCode(value == null ? null : value.name());
    }
}
