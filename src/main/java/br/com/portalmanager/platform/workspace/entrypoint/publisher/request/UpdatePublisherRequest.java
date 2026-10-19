package br.com.portalmanager.platform.workspace.entrypoint.publisher.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.UpdatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.operations.PublisherNormalizer;
import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(type = "PUBLISHER", code = "publisher", normalizer = PublisherNormalizer.class)
public record UpdatePublisherRequest(
    Long version,
    String code,
    String name,
    String description,
    String scope,
    Boolean deprecated,
    JsonNode settings
) {
    public UpdatePublisherInput toInput() {
        return new UpdatePublisherInput(version, code, name, description, scope, deprecated, settings);
    }
}
