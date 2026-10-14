package br.com.portalmanager.platform.workspace.entrypoint.web.environment.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentNormalizer;

@ValidateResourceSchemaRequest(
    type = "ENVIRONMENT",
    code = "environment-type-compatibility",
    normalizer = EnvironmentNormalizer.class
)
public record CreateEnvironmentTypeCompatibilityRequest(
    String parentTypeCode,
    String childTypeCode
) {}
