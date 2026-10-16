package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.message.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageNormalizer;

@ValidateResourceSchemaRequest(
        type = "MESSAGE",
        code = "message",
        normalizer = MessageNormalizer.class)
public record UpdateMessageRequest(
        Long version,
        String microserviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation) {
    public UpdateMessageInput toInput() {
        return new UpdateMessageInput(
                version, microserviceIdentifier, messageKey, code, httpStatus, observation);
    }
}
