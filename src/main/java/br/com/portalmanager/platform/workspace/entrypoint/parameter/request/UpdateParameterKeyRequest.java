package br.com.portalmanager.platform.workspace.entrypoint.parameter.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyRequestNormalizer;

import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(
        type = "PLATFORM",
        code = "dynamic-parameter",
        normalizer = ParameterKeyRequestNormalizer.class)
public record UpdateParameterKeyRequest(
        Long version, String description, String type, JsonNode value) {
    public UpdateParameterKeyInput toInput() {
        return new UpdateParameterKeyInput(version, description, type, value);
    }
}
