package br.com.portalmanager.platform.workspace.entrypoint.web.message.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.MessageNormalizer;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;
import java.util.List;

@ValidateResourceSchemaRequest(type = "MESSAGE", code = "message", normalizer = MessageNormalizer.class)
public record CreateMessageRequest(
    Long version,
    String microserviceIdentifier,
    String messageKey,
    String code,
    Integer httpStatus,
    String observation,
    List<CreateMessageTranslationRequest> translations
) {
    public CreateMessageInput toInput() {
        return new CreateMessageInput(
            version,
            microserviceIdentifier,
            messageKey,
            code,
            httpStatus,
            observation,
            translations == null
                ? null
                : translations
                      .stream()
                      .map(value -> value == null ? null : value.toInput())
                      .toList()
        );
    }
}
