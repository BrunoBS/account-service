package br.com.portalmanager.platform.workspace.foundation.schema.integration;

public interface SchemaResolutionPort {
    String resolve(String resourceType, String resourceCode);
}
