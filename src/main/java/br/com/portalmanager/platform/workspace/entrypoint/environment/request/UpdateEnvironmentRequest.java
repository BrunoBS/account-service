package br.com.portalmanager.platform.workspace.entrypoint.environment.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentNormalizer;
import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(type = "ENVIRONMENT", code = "environment", normalizer = EnvironmentNormalizer.class)
public record UpdateEnvironmentRequest(
    Long version,
    String name,
    String description,
    String authorizationType,
    Integer sortOrder,
    String authorizerGroup,
    JsonNode settings,
    String environmentTypeCode,
    String parentIdentifier
) {
    public UpdateEnvironmentInput toInput() {
        return new UpdateEnvironmentInput(
            version,
            name,
            description,
            authorizationType,
            sortOrder,
            authorizerGroup,
            settings,
            environmentTypeCode,
            parentIdentifier
        );
    }
}
