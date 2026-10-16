package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicparameter.parameterkey.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyNormalizer;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

@ValidateResourceSchemaRequest(
        type = "PLATFORM",
        code = "dynamic-parameter",
        normalizer = ParameterKeyNormalizer.class)
public record CreateParameterKeyRequest(
        Long version,
        UUID microserviceIdentifier,
        String key,
        String description,
        String type,
        JsonNode value) {
    public CreateParameterKeyInput toInput() {
        return new CreateParameterKeyInput(
                version,
                microserviceIdentifier == null ? null : microserviceIdentifier.toString(),
                key,
                description,
                type,
                value);
    }
}
