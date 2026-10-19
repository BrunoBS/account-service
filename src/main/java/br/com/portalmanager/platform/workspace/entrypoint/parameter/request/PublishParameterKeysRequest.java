package br.com.portalmanager.platform.workspace.entrypoint.parameter.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishParameterKeysInput;

import java.util.List;
import java.util.UUID;

@ValidateResourceSchemaRequest(type = "PLATFORM", code = "dynamic-parameter-publication")
public record PublishParameterKeysRequest(List<UUID> identifiers) {
    public PublishParameterKeysInput toInput() {
        return new PublishParameterKeysInput(identifiers.stream().map(UUID::toString).toList());
    }
}
