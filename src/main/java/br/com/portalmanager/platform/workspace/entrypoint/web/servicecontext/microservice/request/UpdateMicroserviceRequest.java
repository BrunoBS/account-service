package br.com.portalmanager.platform.workspace.entrypoint.web.servicecontext.microservice.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.microservice.UpdateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceNormalizer;

import tools.jackson.databind.JsonNode;

@ValidateResourceSchemaRequest(
        type = "SERVICE_CONTEXT",
        code = "microservice",
        normalizer = MicroserviceNormalizer.class)
public record UpdateMicroserviceRequest(
        Long version, String code, String name, String description, JsonNode settings) {
    public UpdateMicroserviceInput toInput() {
        return new UpdateMicroserviceInput(
                version, code, name, description, settings == null ? null : settings.toString());
    }
}
