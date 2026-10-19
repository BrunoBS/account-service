package br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.UpdateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupNormalizer;

@ValidateResourceSchemaRequest(
        type = "SERVICE_CONTEXT",
        code = "feature-group",
        normalizer = FeatureGroupNormalizer.class)
public record UpdateFeatureGroupRequest(Long version, String code, String name, String description) {
    public UpdateFeatureGroupInput toInput() {
        return new UpdateFeatureGroupInput(version, code, name, description);
    }
}
