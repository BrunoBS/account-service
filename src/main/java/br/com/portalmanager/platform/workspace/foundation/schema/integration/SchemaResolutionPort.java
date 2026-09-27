package br.com.portalmanager.platform.workspace.foundation.schema.integration;

public interface SchemaResolutionPort {

    String resolvePlatform(String schemaTypeCode);

    String resolveWorkspace(
            String workspaceIdentifier,
            String schemaTypeCode,
            String schemaCode
    );
}
