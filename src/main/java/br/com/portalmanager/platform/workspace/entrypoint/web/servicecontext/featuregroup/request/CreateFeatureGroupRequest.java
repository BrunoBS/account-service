package br.com.portalmanager.platform.workspace.entrypoint.web.servicecontext.featuregroup.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.CreateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupNormalizer;

@ValidateResourceSchemaRequest(
        type = "SERVICE_CONTEXT",
        code = "feature-group",
        normalizer = FeatureGroupNormalizer.class)
public record CreateFeatureGroupRequest(Long version, String code, String name, String description) {
    public CreateFeatureGroupInput toInput() {
        return new CreateFeatureGroupInput(version, code, name, description);
    }
}
