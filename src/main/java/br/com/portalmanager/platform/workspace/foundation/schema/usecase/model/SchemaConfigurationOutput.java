package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaConfiguration;

public record SchemaConfigurationOutput(String identifier, Long version, String resourceType,
                                        String resourceCode, String schemaIdentifier, String lifecycle) {
    public static SchemaConfigurationOutput from(SchemaConfiguration value) {
        return new SchemaConfigurationOutput(value.getIdentifier(), value.getVersion(),
                value.getResourceType(), value.getResourceCode(),
                value.getSchema().getIdentifier(), value.getLifecycle().value());
    }
}
