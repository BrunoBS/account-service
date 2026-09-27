package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaResolution;

public interface SchemaResolutionPort {

    SchemaResolution resolvePlatform(String schemaTypeCode);

    SchemaResolution resolveWorkspace(
            String workspaceIdentifier,
            String schemaTypeCode,
            String schemaCode
    );
}
