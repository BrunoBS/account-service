package br.com.portalmanager.platform.workspace.entrypoint.web.application.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationNormalizer;
import java.util.List;
import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(type = "APPLICATION", code = "application", normalizer = ApplicationNormalizer.class)
public record UpdateApplicationRequest(
    Long version,
    String name,
    String alias,
    String description,
    String acronym,
    String applicationScope,
    String authorizerGroup,
    JsonNode settings,
    List<String> tags
) {
    public UpdateApplicationInput toInput() {
        return new UpdateApplicationInput(
            version,
            name,
            alias,
            description,
            acronym,
            applicationScope,
            authorizerGroup,
            settings,
            tags
        );
    }
}
