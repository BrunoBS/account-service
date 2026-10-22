package br.com.portalmanager.platform.workspace.foundation.schema.integration;

public interface SchemaResolutionPort {
    String resolveRequired(String resourceType, String resourceCode);

    String resolve(String resourceType, String resourceCode);
}
