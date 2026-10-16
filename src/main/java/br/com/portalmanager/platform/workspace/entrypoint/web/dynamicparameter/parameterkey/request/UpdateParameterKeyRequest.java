package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicparameter.parameterkey.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyNormalizer;

import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(
        type = "PLATFORM",
        code = "dynamic-parameter",
        normalizer = ParameterKeyNormalizer.class)
public record UpdateParameterKeyRequest(
        Long version, String description, String type, JsonNode value) {
    public UpdateParameterKeyInput toInput() {
        return new UpdateParameterKeyInput(version, description, type, value);
    }
}
