package br.com.portalmanager.platform.workspace.core.workspace.usecase.model;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

public record FindAllWorkspacesInput(
        LifecycleTypeCode lifecycle,
        String typeName,
        String tagName
) {
}
