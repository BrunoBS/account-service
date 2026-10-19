package br.com.portalmanager.platform.workspace.entrypoint.environment.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentNormalizer;

@ValidateResourceSchemaRequest(type = "ENVIRONMENT", code = "environment-type", normalizer = EnvironmentNormalizer.class)
public record CreateEnvironmentTypeRequest(
    Long version,
    String code,
    String name,
    String description,
    Boolean rootAllowed,
    Boolean allowsChildren,
    Boolean workspaceRequired,
    Integer displayOrder
) {
    public EnvironmentTypeInput toInput() {
        return new EnvironmentTypeInput(
            version,
            code,
            name,
            description,
            rootAllowed,
            allowsChildren,
            workspaceRequired,
            displayOrder
        );
    }
}
