package br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain;

import br.com.portalmanager.platform.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class WorkspaceTypeCode extends AbstractCatalogCode {

    protected WorkspaceTypeCode() {
    }

    private WorkspaceTypeCode(String value) {
        super(value);
    }

    private WorkspaceTypeCode(WorkspaceTypeEnum value) {
        super(value);
    }

    public static WorkspaceTypeCode of(String value) {
        return new WorkspaceTypeCode(value);
    }

    public static WorkspaceTypeCode of(WorkspaceTypeEnum value) {
        return new WorkspaceTypeCode(value);
    }

    public static boolean isValidFormat(String value) {
        return AbstractCatalogCode.isValidFormat(value)
                && value.equals(value.toUpperCase(java.util.Locale.ROOT));
    }
}
