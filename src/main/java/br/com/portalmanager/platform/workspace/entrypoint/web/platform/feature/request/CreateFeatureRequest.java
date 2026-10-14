package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureNormalizer;
import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(type = "PLATFORM", code = "feature", normalizer = FeatureNormalizer.class)
public record CreateFeatureRequest(
    Long version, String code, String name, String description,
    String microserviceIdentifier, JsonNode settings, boolean shareable
) {
    public CreateFeatureInput toInput() {
        return new CreateFeatureInput(version, code, name, description, microserviceIdentifier,
            settings == null ? null : settings.toString(), shareable);
    }
}
