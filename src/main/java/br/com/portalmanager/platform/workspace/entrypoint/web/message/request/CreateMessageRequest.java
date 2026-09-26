package br.com.portalmanager.platform.workspace.entrypoint.web.message.request;

import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;

import java.util.List;

public record CreateMessageRequest(
        String serviceIdentifier,
        String messageKey,
        String code,
        Integer httpStatus,
        String observation,
        List<CreateMessageTranslationRequest> translations
) {
    public CreateMessageInput toInput() {
        return new CreateMessageInput(
                serviceIdentifier,
                messageKey,
                code,
                httpStatus,
                observation,
                translations == null ? null : translations.stream()
                        .map(value -> value == null ? null : value.toInput())
                        .toList()
        );
    }
}
