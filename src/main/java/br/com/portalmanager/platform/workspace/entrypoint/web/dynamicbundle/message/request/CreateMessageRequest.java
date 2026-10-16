package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.message.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.messagetranslator.request.CreateMessageTranslatorRequest;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageNormalizer;

import java.util.List;

@ValidateResourceSchemaRequest(
        type = "MESSAGE",
        code = "message",
        normalizer = MessageNormalizer.class)
public record CreateMessageRequest(
        Long version,
        String microserviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation,
        List<CreateMessageTranslatorRequest> translators) {
    public CreateMessageInput toInput() {
        return new CreateMessageInput(
                version,
                microserviceIdentifier,
                messageKey,
                code,
                httpStatus,
                observation,
                translators == null
                        ? null
                        : translators.stream()
                                .map(value -> value == null ? null : value.toInput())
                                .toList());
    }
}
