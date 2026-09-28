package br.com.portalmanager.platform.workspace.foundation.schema.integration;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaResourceType;

public interface SchemaResolutionPort {
    String resolve(SchemaResourceType resourceType, String resourceCode);
    String resolveWorkspace(String workspaceIdentifier, String schemaCode);
}
