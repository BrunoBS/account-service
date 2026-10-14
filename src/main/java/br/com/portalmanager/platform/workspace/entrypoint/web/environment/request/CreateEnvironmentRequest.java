package br.com.portalmanager.platform.workspace.entrypoint.web.environment.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentNormalizer;
import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(type = "ENVIRONMENT", code = "environment", normalizer = EnvironmentNormalizer.class)
public record CreateEnvironmentRequest(
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
    public CreateEnvironmentInput toInput() {
        return new CreateEnvironmentInput(
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
